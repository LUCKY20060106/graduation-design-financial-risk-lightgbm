import pandas as pd
import os

# 配置路径
DATA_DIR = r'd:\桌面\计算机科学与技术毕业论文\偿债能力215502592'
FILE_PATH = os.path.join(DATA_DIR, 'FI_T1.xlsx')

def preprocess_solvency_data(file_path):
    print(f"正在读取数据: {file_path}")
    
    # 1. 读取 Excel 文件
    # 注意：如果运行报错，可能需要安装 openpyxl: pip install openpyxl
    df = pd.read_excel(file_path)
    original_count = len(df)
    print(f"原始数据条数: {original_count}")

    # 2. 仅保留合并报表 (Typrep == 'A')
    df_filtered = df[df['Typrep'] == 'A'].copy()
    print(f"保留合并报表后条数: {len(df_filtered)} (剔除母公司报表 {original_count - len(df_filtered)} 条)")

    # 3. 仅保留 12 月 31 日的年报 (Accper 结尾为 -12-31)
    # 先确保 Accper 是字符串格式
    df_filtered['Accper'] = df_filtered['Accper'].astype(str)
    df_filtered = df_filtered[df_filtered['Accper'].str.endswith('-12-31')].copy()
    print(f"保留 12 月 31 日年报后条数: {len(df_filtered)}")

    # 4. 统计缺失值情况
    print("\n各指标缺失值统计 (填充前):")
    feature_cols = [
        'F010101A', 'F010201A', 'F010401A', 'F010601A', 'F010701B', 
        'F010801B', 'F010901B', 'F011201A', 'F011301A', 'F011401A', 'F011601A'
    ]
    
    missing_stats = df_filtered[feature_cols].isnull().sum()
    missing_ratio = (missing_stats / len(df_filtered)) * 100
    
    stats_df = pd.DataFrame({
        '缺失数量': missing_stats,
        '缺失比例(%)': missing_ratio.round(2)
    })
    print(stats_df)

    # 5. 缺失值处理：中位数填充
    print("\n正在进行中位数填充...")
    for col in feature_cols:
        median_val = df_filtered[col].median()
        df_filtered[col] = df_filtered[col].fillna(median_val)
    
    print("填充完成，再次检查缺失值:")
    print(df_filtered[feature_cols].isnull().sum().sum())

    # 6. 数据持久化：保存为 pickle 格式
    # 存放在根目录，方便后续多表合并
    output_path = r'd:\桌面\计算机科学与技术毕业论文\solvency_features.pkl'
    df_filtered.to_pickle(output_path)
    print(f"\n清洗后的偿债能力特征已保存至: {output_path}")
    
    return df_filtered

if __name__ == "__main__":
    if os.path.exists(FILE_PATH):
        clean_df = preprocess_solvency_data(FILE_PATH)
    else:
        print(f"错误: 找不到文件 {FILE_PATH}")
