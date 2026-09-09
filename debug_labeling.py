import pandas as pd
import os

base_path = r'd:\桌面\计算机科学与技术毕业论文'
features_path = os.path.join(base_path, 'merged_features.pkl')
st_path = os.path.join(base_path, 'RESSET 股票_股票特别处理与撤销', 'RESSET_STKSPCTRMT_1.xlsx')

print("--- 特征表样本 ---")
df = pd.read_pickle(features_path)
print("列名:", df.columns.tolist())
print("Stkcd 示例:", df['Stkcd'].head().tolist())
print("Accper 示例:", df['Accper'].head().tolist())

print("\n--- ST记录表样本 ---")
st_df = pd.read_excel(st_path)
print("列名:", st_df.columns.tolist())
# 使用清洗后的列名逻辑
st_df.columns = [str(c).replace(' ', '').replace('\xa0', '') for c in st_df.columns]
col_stkcd = [c for c in st_df.columns if 'StkCd' in c][0]
col_trmt = [c for c in st_df.columns if 'TrmtType' in c][0]
print("StkCd 示例:", st_df[col_stkcd].head().tolist())
print("TrmtType 取值分布:\n", st_df[col_trmt].value_counts())
