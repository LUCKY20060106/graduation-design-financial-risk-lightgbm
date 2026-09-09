import pandas as pd
import numpy as np
import lightgbm as lgb
import pickle
import json
import os
from sklearn.metrics import roc_curve, auc, precision_recall_curve
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier

def compare_models():
    ready_data_path = 'model_ready_data.pkl'
    lgb_model_path = 'lgb_model.txt'
    output_path = 'comparison_metrics.json'

    if not os.path.exists(ready_data_path) or not os.path.exists(lgb_model_path):
        print("Error: Required files missing.")
        return

    # 1. Load data
    with open(ready_data_path, 'rb') as f:
        data = pickle.load(f)
    X_train = data['X_train']
    y_train = data['y_train']
    X_test = data['X_test']
    y_test = data['y_test']

    # 2. Models to compare
    results = {}

    # --- LightGBM ---
    lgb_model = lgb.Booster(model_file=lgb_model_path)
    y_prob_lgb = lgb_model.predict(X_test)
    fpr_lgb, tpr_lgb, _ = roc_curve(y_test, y_prob_lgb)
    results["LightGBM (Improved)"] = {
        "fpr": fpr_lgb.tolist()[::15],
        "tpr": tpr_lgb.tolist()[::15],
        "auc": float(auc(fpr_lgb, tpr_lgb))
    }

    # --- Logistic Regression (Baseline) ---
    lr = LogisticRegression(max_iter=1000)
    lr.fit(X_train, y_train)
    y_prob_lr = lr.predict_proba(X_test)[:, 1]
    fpr_lr, tpr_lr, _ = roc_curve(y_test, y_prob_lr)
    results["Logistic Regression"] = {
        "fpr": fpr_lr.tolist()[::15],
        "tpr": tpr_lr.tolist()[::15],
        "auc": float(auc(fpr_lr, tpr_lr))
    }

    # --- Random Forest ---
    rf = RandomForestClassifier(n_estimators=100, random_state=42)
    rf.fit(X_train, y_train)
    y_prob_rf = rf.predict_proba(X_test)[:, 1]
    fpr_rf, tpr_rf, _ = roc_curve(y_test, y_prob_rf)
    results["Random Forest"] = {
        "fpr": fpr_rf.tolist()[::15],
        "tpr": tpr_rf.tolist()[::15],
        "auc": float(auc(fpr_rf, tpr_rf))
    }

    # 3. Save JSON
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=4)
    
    print(f"Comparison metrics saved to: {output_path}")

if __name__ == "__main__":
    compare_models()
