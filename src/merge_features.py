import pandas as pd
import os

def load_and_clean_financial_data(file_path, sheet_name=0):
    """读取并初步清洗财务报表数据"""
    print(f"正在处理: {os.path.basename(file_path)}")
    df = pd.read_excel(file_path, sheet_name=sheet_name)
    
    # 记录原始大小
    original_len = len(df)
    
    # 1. 仅保留合并报表 (Typrep == 'A')
    if 'Typrep' in df.columns:
        df = df[df['Typrep'] == 'A']
    
    # 2. 仅保留12月31日年报
    if 'Accper' in df.columns:
        df = df[df['Accper'].astype(str).str.endswith('12-31')]
        
    print(f"  -> 过滤后条数: {len(df)} (剔除 {original_len - len(df)} 条)")
    return df

# 定义路径
base_path = r'd:\桌面\计算机科学与技术毕业论文'
solvency_path = os.path.join(base_path, '偿债能力215502592', 'FI_T1.xlsx')
operation_path = os.path.join(base_path, '经营能力151930851(仅供华南理工大学使用)', 'FI_T4.xlsx')
growth_path = os.path.join(base_path, '发展能力152411153(仅供华南理工大学使用)', 'FI_T8.xlsx')

# 1. 分别加载并清洗
df_solvency = load_and_clean_financial_data(solvency_path)
df_operation = load_and_clean_financial_data(operation_path)
df_growth = load_and_clean_financial_data(growth_path)

# 2. 合并数据
# 使用 Stkcd (股票代码) 和 Accper (会计期间) 作为唯一主键进行左连接
print("\n正在合并各维度指标...")
final_df = df_solvency.merge(df_operation, on=['Stkcd', 'Accper'], how='left', suffixes=('', '_op'))
final_df = final_df.merge(df_growth, on=['Stkcd', 'Accper'], how='left', suffixes=('', '_gr'))

# 剔除重复的辅助列 (如 ShortName, Typrep 等在合并时产生的重复项)
cols_to_keep = [c for c in final_df.columns if not c.endswith(('_op', '_gr'))]
final_df = final_df[cols_to_keep]

print(f"合并完成！最终特征维度: {final_df.shape}")

# 3. 保存中间结果
output_path = os.path.join(base_path, 'merged_features.pkl')
final_df.to_pickle(output_path)
print(f"已保存合并后的特征表至: {output_path}")
