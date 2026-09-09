import lightgbm as lgb
import pandas as pd
import numpy as np
import os
import pickle
import sys
import json
import shap

def predict(input_data, threshold_high=0.7, threshold_medium=0.3):
    """
    input_data: list of feature values in the correct order
    """
    model_path = 'lgb_model.txt' # 使用相对路径避免中文编码问题
    
    if not os.path.exists(model_path):
        # 尝试兼容从 src 目录运行的情况
        alt_path = os.path.join('..', 'lgb_model.txt')
        if os.path.exists(alt_path):
            model_path = alt_path
        else:
            return {"error": "模型文件不存在，请先运行训练脚本。"}

    # 1. 加载模型
    model = lgb.Booster(model_file=model_path)
    
    # 2. 准备预测数据
    # 确保输入是 2D array
    features = np.array(input_data).reshape(1, -1)
    
    # 3. 进行预测
    prob = model.predict(features)[0]
    
    # 4. 计算 SHAP 值 (解释性分析)
    # 使用 TreeExplainer 对 LightGBM 进行解释
    explainer = shap.TreeExplainer(model)
    shap_values = explainer.shap_values(features)
    
    # 对于二分类，shap_values 可能是一个列表 [neg_shap, pos_shap] 或者直接是 pos_shap
    # 取正类（风险）的贡献值
    if isinstance(shap_values, list):
        current_shap = shap_values[1][0]
    else:
        current_shap = shap_values[0]
        
    # 特征名称（必须与训练时的顺序完全一致）
    feature_names = [
        '流动比率', '速动比率', '现金比率', '营运资金', '利息保障倍数', 
        '经营净现金流/流动负债', '现金流利息保障倍数', '资产负债率', '有形资产负债率', '权益乘数',
        '应收账款周转率', '存货周转率', '流动资产周转率', '总资产周转率',
        '总资产增长率', '净利润增长率', '营业收入增长率'
    ]
    
    # 5. 返回结果
    result = {
        "risk_probability": float(prob),
        "is_risk": bool(prob > threshold_medium),
        "level": "高风险" if prob > threshold_high else ("中风险" if prob > threshold_medium else "低风险"),
        "shap_analysis": dict(zip(feature_names, [float(x) for x in current_shap])),
        "feature_values": dict(zip(feature_names, [float(x) for x in input_data])) # 添加特征值及其名称
    }
    return result

if __name__ == "__main__":
    # 优先从标准输入读取数据，如果标准输入为空则尝试从命令行参数读取
    input_str = ""
    if not sys.stdin.isatty():
        input_str = sys.stdin.read().strip()
    
    if not input_str and len(sys.argv) > 1:
        input_str = sys.argv[1]

    if input_str:
        try:
            payload = json.loads(input_str)
            if isinstance(payload, dict) and "features" in payload:
                res = predict(
                    payload["features"], 
                    payload.get("threshold_high", 0.7), 
                    payload.get("threshold_medium", 0.3)
                )
            else:
                res = predict(payload)
            print(json.dumps(res))
        except Exception as e:
            print(json.dumps({"error": str(e), "raw_input": input_str}))
    else:
        # 演示模式：从测试集中随机抽取一条数据进行预测
        base_path = r'd:\桌面\计算机科学与技术毕业论文'
        ready_data_path = os.path.join(base_path, 'model_ready_data.pkl')
        
        if os.path.exists(ready_data_path):
            with open(ready_data_path, 'rb') as f:
                data = pickle.load(f)
            
            X_test = data['X_test']
            sample_idx = np.random.randint(0, len(X_test))
            sample_features = X_test.iloc[sample_idx].tolist()
            
            print(f"正在对测试集第 {sample_idx} 条样本进行演示预测...")
            print(f"特征值: {sample_features}")
            
            res = predict(sample_features)
            print("\n预测结果:")
            print(json.dumps(res, indent=4, ensure_ascii=False))
        else:
            print("未找到测试数据，请先运行特征工程脚本。")
