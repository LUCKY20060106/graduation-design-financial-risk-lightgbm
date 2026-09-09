import pandas as pd
import numpy as np
import os
import pickle
import lightgbm as lgb
from sklearn.metrics import classification_report, roc_auc_score, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns

def train_model():
    base_path = r'd:\桌面\计算机科学与技术毕业论文'
    input_path = os.path.join(base_path, 'model_ready_data.pkl')
    
    print("正在加载就绪的数据集...")
    with open(input_path, 'rb') as f:
        data = pickle.load(f)
    
    X_train = data['X_train']
    y_train = data['y_train']
    X_test = data['X_test']
    y_test = data['y_test']
    feature_names = data['feature_names']
    
    print(f"训练集规模: {X_train.shape}, 测试集规模: {X_test.shape}")
    
    # 1. 创建 LightGBM 数据集格式
    train_data = lgb.Dataset(X_train, label=y_train, feature_name=feature_names)
    test_data = lgb.Dataset(X_test, label=y_test, reference=train_data, feature_name=feature_names)
    
    # 2. 设置参数
    # 这些参数是基于财务预警任务的常用推荐值
    params = {
        'boosting_type': 'gbdt',
        'objective': 'binary',
        'metric': 'auc',
        'learning_rate': 0.05,
        'num_leaves': 31,
        'max_depth': 6,
        'feature_fraction': 0.8,
        'bagging_fraction': 0.8,
        'bagging_freq': 5,
        'verbose': -1,
        'random_state': 42,
        'is_unbalance': False # 因为我们已经用了 SMOTE，所以这里设为 False
    }
    
    # 3. 训练模型 (加入 Early Stopping)
    print("正在启动 LightGBM 训练...")
    model = lgb.train(
        params,
        train_data,
        num_boost_round=1000,
        valid_sets=[train_data, test_data],
        valid_names=['train', 'valid'],
        callbacks=[
            lgb.early_stopping(stopping_rounds=50),
            lgb.log_evaluation(period=50)
        ]
    )
    
    # 4. 模型评估
    print("\n--- 模型评估报告 ---")
    y_pred_prob = model.predict(X_test)
    y_pred = (y_pred_prob > 0.5).astype(int)
    
    print("\n分类报告:")
    print(classification_report(y_test, y_pred))
    
    auc = roc_auc_score(y_test, y_pred_prob)
    print(f"AUC 得分: {auc:.4f}")
    
    # 5. 保存模型
    model_save_path = 'lgb_model.txt' # 使用相对路径避免中文乱码问题
    model.save_model(model_save_path)
    print(f"\n模型已保存至当前目录下的: {model_save_path}")
    
    # 6. 特征重要性
    importance = pd.DataFrame({
        'feature': feature_names,
        'importance': model.feature_importance(importance_type='gain')
    }).sort_values(by='importance', ascending=False)
    
    print("\n前 5 名核心指标:")
    print(importance.head(5))

if __name__ == "__main__":
    train_model()
