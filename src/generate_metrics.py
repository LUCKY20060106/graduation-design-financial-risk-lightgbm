import pandas as pd
import numpy as np
import lightgbm as lgb
import pickle
import json
import os
from sklearn.metrics import roc_curve, auc, confusion_matrix, classification_report

def generate_metrics():
    # 使用相对路径避免 LightGBM 中文编码问题
    model_path = 'lgb_model.txt'
    ready_data_path = 'model_ready_data.pkl'
    output_path = 'metrics.json'

    if not os.path.exists(ready_data_path) or not os.path.exists(model_path):
        print(f"错误: 文件不存在. Model: {os.path.abspath(model_path)}, Data: {os.path.abspath(ready_data_path)}")
        return

    # 1. 加载数据
    with open(ready_data_path, 'rb') as f:
        data = pickle.load(f)
    X_test = data['X_test']
    y_test = data['y_test']

    # 2. 加载模型
    model = lgb.Booster(model_file=model_path)

    # 3. 预测概率
    y_prob = model.predict(X_test)
    y_pred = (y_prob > 0.5).astype(int)

    # 4. 计算 ROC 曲线
    fpr, tpr, thresholds = roc_curve(y_test, y_prob)
    roc_auc = auc(fpr, tpr)

    # 5. 计算混淆矩阵
    cm = confusion_matrix(y_test, y_pred)
    
    # 6. 分类报告
    report = classification_report(y_test, y_pred, output_dict=True)
    
    # 兼容不同的 key (可能为 '1' 或 '1.0')
    pos_label = '1' if '1' in report else '1.0'

    # 7. 整理结果
    metrics = {
        "auc": float(roc_auc),
        "roc_curve": {
            "fpr": fpr.tolist()[::10], # 抽样以减小体积
            "tpr": tpr.tolist()[::10]
        },
        "confusion_matrix": cm.tolist(),
        "classification_report": report,
        "overall": {
            "accuracy": report['accuracy'],
            "precision": report[pos_label]['precision'],
            "recall": report[pos_label]['recall'],
            "f1_score": report[pos_label]['f1-score']
        }
    }

    # 8. 保存 JSON
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(metrics, f, ensure_ascii=False, indent=4)
    
    print(f"评估指标已生成至: {output_path}")

if __name__ == "__main__":
    generate_metrics()
