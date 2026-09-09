import pandas as pd
import numpy as np
import os
from sklearn.model_selection import train_test_split
from sklearn.feature_selection import VarianceThreshold
from imblearn.over_sampling import SMOTE
from sklearn.impute import SimpleImputer
import pickle

def feature_engineering():
    base_path = r'd:\桌面\计算机科学与技术毕业论文'
    input_path = os.path.join(base_path, 'final_labeled_data.pkl')
    
    print("正在加载已打标的数据...")
    df = pd.read_pickle(input_path)
    
    # 1. 准备特征矩阵 X 和 标签向量 y
    # 排除非特征列 (Stkcd, ShortName, Accper, Typrep, Source, Label, Year)
    exclude_cols = ['Stkcd', 'ShortName', 'Accper', 'Typrep', 'Source', 'Label', 'Year']
    feature_cols = [c for c in df.columns if c not in exclude_cols]
    
    X = df[feature_cols]
    y = df['Label']
    
    print(f"原始特征数量: {len(feature_cols)}")
    
    # 2. 缺失值填充 (针对合并后新产生的缺失值)
    # SMOTE 不支持 NaN，因此必须在此处进行填充
    print("正在进行缺失值中位数填充...")
    imputer = SimpleImputer(strategy='median')
    X_imputed = imputer.fit_transform(X)
    X = pd.DataFrame(X_imputed, columns=feature_cols)

    # 3. 特征选择 (Variance Threshold)
    # 删除方差低于 0.01 的特征
    print("正在进行方差筛选...")
    selector = VarianceThreshold(threshold=0.01)
    X_selected_arr = selector.fit_transform(X)
    selected_features = X.columns[selector.get_support()].tolist()
    X_selected = pd.DataFrame(X_selected_arr, columns=selected_features)
    
    print(f"方差筛选后保留的特征数量: {len(selected_features)}")
    print(f"剔除的特征: {set(feature_cols) - set(selected_features)}")
    
    # 4. 数据划分 (按时间划分以防信息泄露)
    df['Year'] = pd.to_datetime(df['Accper']).dt.year
    train_idx = (df['Year'] <= 2022).values
    test_idx = (df['Year'] > 2022).values
    
    X_train = X_selected.iloc[train_idx].reset_index(drop=True)
    y_train = y.iloc[train_idx].reset_index(drop=True)
    X_test = X_selected.iloc[test_idx].reset_index(drop=True)
    y_test = y.iloc[test_idx].reset_index(drop=True)
    
    print(f"训练集样本量: {len(X_train)} (ST: {sum(y_train)})")
    print(f"测试集样本量: {len(X_test)} (ST: {sum(y_test)})")
    
    # 5. 类别不平衡处理 (SMOTE)
    # 仅对训练集进行过采样
    print("正在对训练集进行 SMOTE 过采样以平衡类别...")
    smote = SMOTE(random_state=42)
    X_train_res, y_train_res = smote.fit_resample(X_train, y_train)
    
    print(f"过采样后训练集样本量: {len(X_train_res)} (ST: {sum(y_train_res)})")
    
    # 6. 保存处理后的数据集
    output = {
        'X_train': X_train_res,
        'y_train': y_train_res,
        'X_test': X_test,
        'y_test': y_test,
        'feature_names': selected_features
    }
    
    output_path = os.path.join(base_path, 'model_ready_data.pkl')
    with open(output_path, 'wb') as f:
        pickle.dump(output, f)
        
    print(f"特征工程完成！已保存模型就绪数据至: {output_path}")

if __name__ == "__main__":
    feature_engineering()
