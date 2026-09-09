import pandas as pd
import os

def label_financial_data():
    base_path = r'd:\桌面\计算机科学与技术毕业论文'
    features_path = os.path.join(base_path, 'merged_features.pkl')
    st_path = os.path.join(base_path, 'RESSET 股票_股票特别处理与撤销', 'RESSET_STKSPCTRMT_1.xlsx')

    print("正在加载合并后的特征数据...")
    df = pd.read_pickle(features_path)
    
    print("正在加载ST记录数据...")
    st_df = pd.read_excel(st_path)

    # 强力清洗列名：去除所有空格和特殊字符
    st_df.columns = [str(c).replace(' ', '').replace('\xa0', '') for c in st_df.columns]
    
    # 根据 RESSET 常见编码定义
    # 1, 3, 5, 7: 实施 ST/PT/*ST/其他
    # 2, 4, 6, 8: 撤销 ST/PT/*ST/其他
    apply_codes = [1, 3, 5, 7, 9, 11, 13, 15]
    remove_codes = [2, 4, 6, 8, 10, 12, 14]

    # 寻找列名
    def find_col(keyword):
        for col in st_df.columns:
            if keyword in col:
                return col
        return None

    col_stkcd = find_col('StkCd')
    col_impdt = find_col('ImpDt')
    col_trmttype = find_col('TrmtType')

    print(f"匹配到的字段: 代码->{col_stkcd}, 日期->{col_impdt}, 类型->{col_trmttype}")

    # 处理日期和年份
    st_df[col_impdt] = pd.to_datetime(st_df[col_impdt])
    st_df['Year'] = st_df[col_impdt].dt.year
    
    df['Year'] = pd.to_datetime(df['Accper']).dt.year
    
    # 统一代码格式为6位字符串
    df['Stkcd_str'] = df['Stkcd'].astype(str).str.zfill(6)
    st_df['Stkcd_str'] = st_df[col_stkcd].astype(str).str.zfill(6)

    # 标记 ST 标签，初始为 0
    df['Label'] = 0
    
    print("正在进行时序匹配打标...")
    # 按照时间顺序处理 ST 记录
    st_df = st_df.sort_values(by=col_impdt)
    
    # 为了提高效率，按公司处理
    unique_st_companies = st_df['Stkcd_str'].unique()
    
    for stkcd in unique_st_companies:
        company_st_history = st_df[st_df['Stkcd_str'] == stkcd]
        
        for _, row in company_st_history.iterrows():
            imp_year = row['Year']
            trmt_code = row[col_trmttype]
            
            if trmt_code in apply_codes:
                # 从实施年开始标记为 1
                df.loc[(df['Stkcd_str'] == stkcd) & (df['Year'] >= imp_year), 'Label'] = 1
            elif trmt_code in remove_codes:
                # 从撤销年之后标记为 0
                # 注意：撤销当年通常还保留ST标记一段时间，通常从下一年开始算正常
                df.loc[(df['Stkcd_str'] == stkcd) & (df['Year'] > imp_year), 'Label'] = 0

    print(f"打标完成！Label 分布情况:")
    label_counts = df['Label'].value_counts()
    print(label_counts)
    
    if 1 in label_counts:
        print(f"检测到 {label_counts[1]} 条 ST 风险样本，占比 {label_counts[1]/len(df)*100:.2f}%")
    else:
        print("警告：未检测到任何 ST 样本，请检查匹配逻辑或原始数据。")
    
    # 删除临时辅助列
    df = df.drop(columns=['Stkcd_str', 'Year'])
    
    # 保存结果
    output_path = os.path.join(base_path, 'final_labeled_data.pkl')
    df.to_pickle(output_path)
    print(f"已保存打标后的数据至: {output_path}")

if __name__ == "__main__":
    label_financial_data()
