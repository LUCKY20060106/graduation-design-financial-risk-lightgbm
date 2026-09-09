import pandas as pd
import json
import os

# 配置路径
PKL_PATH = r'd:\桌面\计算机科学与技术毕业论文\solvency_features.pkl'

def extract_year_stats(pkl_path):
    if not os.path.exists(pkl_path):
        print(f"Error: {pkl_path} not found")
        return
    
    # 读取数据
    df = pd.read_pickle(pkl_path)
    
    # 提取年份
    df['Year'] = df['Accper'].str[:4]
    
    # 计算每年的平均资产负债率 (F011201A) 和 平均流动比率 (F010101A)
    # 过滤掉一些极端异常值以保证图表美观
    stats = df.groupby('Year').agg({
        'F011201A': 'mean',
        'F010101A': 'mean'
    }).reset_index()
    
    # 转换为 JSON 格式打印，方便我复制到后端代码里作为演示数据
    result = {
        "years": stats['Year'].tolist(),
        "debt_ratio": stats['F011201A'].round(4).tolist(),
        "current_ratio": stats['F010101A'].round(4).tolist()
    }
    
    print("--- 统计数据提取成功 ---")
    print(json.dumps(result))
    return result

if __name__ == "__main__":
    extract_year_stats(PKL_PATH)
