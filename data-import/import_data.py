import sys
from pathlib import Path

import pymysql

PROJECT_ROOT = Path(__file__).resolve().parent.parent


def load_env():
    env_path = PROJECT_ROOT / ".env"
    if not env_path.exists():
        print(f"[ERROR] .env 文件不存在: {env_path}")
        sys.exit(1)

    env_vars = {}
    with open(env_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" in line:
                key, _, value = line.partition("=")
                env_vars[key.strip()] = value.strip()
    return env_vars


def get_connection(env):
    return pymysql.connect(
        host=env["DB_HOST"],
        port=int(env.get("DB_PORT", 3306)),
        user=env["DB_USERNAME"],
        password=env["DB_PASSWORD"],
        database=env["DB_NAME"],
        charset="utf8mb4",
    )


STUDENTS = [
    ("20240001", "张三", "计算机科学与技术", 2024),
    ("20240002", "李四", "软件工程", 2024),
    ("20240003", "王五", "数据科学", 2024),
    ("20240004", "赵六", "计算机科学与技术", 2024),
    ("20240005", "孙七", "软件工程", 2024),
    ("20240006", "周八", "人工智能", 2024),
    ("20240007", "吴九", "数据科学", 2024),
    ("20240008", "郑十", "计算机科学与技术", 2024),
    ("20240009", "钱十一", "软件工程", 2024),
    ("20240010", "陈十二", "人工智能", 2024),
    ("20240011", "刘十三", "数据科学", 2024),
    ("20240012", "黄十四", "计算机科学与技术", 2024),
    ("20240013", "林十五", "软件工程", 2024),
    ("20240014", "杨十六", "人工智能", 2024),
    ("20240015", "何十七", "数据科学", 2024),
    ("20230001", "马十八", "计算机科学与技术", 2023),
    ("20230002", "高十九", "软件工程", 2023),
    ("20230003", "罗二十", "人工智能", 2023),
    ("20230004", "梁二一", "数据科学", 2023),
    ("20230005", "宋二二", "计算机科学与技术", 2023),
    ("20230006", "唐二三", "软件工程", 2023),
    ("20230007", "许二四", "人工智能", 2023),
    ("20230008", "韩二五", "数据科学", 2023),
    ("20230009", "冯二六", "计算机科学与技术", 2023),
    ("20230010", "邓二七", "软件工程", 2023),
    ("20220001", "曹二八", "计算机科学与技术", 2022),
    ("20220002", "彭二九", "软件工程", 2022),
    ("20220003", "曾三十", "人工智能", 2022),
    ("20220004", "萧三一", "数据科学", 2022),
    ("20220005", "田三二", "计算机科学与技术", 2022),
]

COURSES = [
    ("CS101", "Java程序设计", "张教授", 3.0, 100, 100, 0),
    ("CS102", "Python程序设计", "李教授", 3.0, 120, 120, 0),
    ("CS103", "数据结构与算法", "王教授", 4.0, 80, 80, 0),
    ("CS104", "操作系统原理", "赵教授", 4.0, 80, 80, 0),
    ("CS105", "计算机网络", "孙教授", 3.5, 90, 90, 0),
    ("CS106", "数据库系统概论", "周教授", 3.5, 90, 90, 0),
    ("CS107", "计算机组成原理", "吴教授", 4.0, 70, 70, 0),
    ("CS108", "编译原理", "郑教授", 3.5, 60, 60, 0),
    ("CS109", "软件工程导论", "钱教授", 3.0, 100, 100, 0),
    ("CS110", "人工智能导论", "陈教授", 3.0, 100, 100, 0),
    ("CS111", "机器学习", "刘教授", 3.5, 80, 80, 0),
    ("CS112", "深度学习", "黄教授", 3.5, 70, 70, 0),
    ("CS113", "自然语言处理", "林教授", 3.0, 60, 60, 0),
    ("CS114", "计算机视觉", "杨教授", 3.0, 60, 60, 0),
    ("CS115", "大数据技术", "何教授", 3.5, 80, 80, 0),
    ("CS116", "云计算与分布式系统", "马教授", 3.5, 70, 70, 0),
    ("CS117", "网络安全", "高教授", 3.0, 80, 80, 0),
    ("CS118", "嵌入式系统", "罗教授", 3.0, 50, 50, 0),
    ("CS119", "Linux系统编程", "梁教授", 3.0, 60, 60, 0),
    ("CS120", "Web前端开发", "宋教授", 2.5, 100, 100, 0),
    ("MATH101", "高等数学（上）", "唐教授", 5.0, 150, 150, 0),
    ("MATH102", "高等数学（下）", "唐教授", 5.0, 150, 150, 0),
    ("MATH103", "线性代数", "许教授", 4.0, 120, 120, 0),
    ("MATH104", "概率论与数理统计", "韩教授", 4.0, 120, 120, 0),
    ("MATH105", "离散数学", "冯教授", 3.5, 100, 100, 0),
    ("ENG101", "大学英语（一）", "邓教授", 2.0, 200, 200, 0),
    ("ENG102", "大学英语（二）", "邓教授", 2.0, 200, 200, 0),
    ("ENG103", "科技英语写作", "曹教授", 2.0, 80, 80, 0),
    ("PE101", "大学体育（一）", "彭教授", 1.0, 300, 300, 0),
    ("PE102", "大学体育（二）", "彭教授", 1.0, 300, 300, 0),
]


def import_students(cursor):
    sql = """
        INSERT IGNORE INTO student (student_no, name, major, grade)
        VALUES (%s, %s, %s, %s)
    """
    count = 0
    for s in STUDENTS:
        cursor.execute(sql, s)
        if cursor.rowcount > 0:
            count += 1
    return count


def import_courses(cursor):
    sql = """
        INSERT IGNORE INTO course (course_no, course_name, teacher, credit, total_capacity, remaining, selected_count)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
    """
    count = 0
    for c in COURSES:
        cursor.execute(sql, c)
        if cursor.rowcount > 0:
            count += 1
    return count


def main():
    print("=" * 50)
    print("  选课平台 - 数据导入脚本")
    print("=" * 50)

    env = load_env()
    print(f"\n[INFO] 数据库: {env['DB_HOST']}:{env['DB_PORT']}/{env['DB_NAME']}")

    conn = get_connection(env)
    cursor = conn.cursor()

    try:
        student_count = import_students(cursor)
        conn.commit()
        print(f"\n[OK] 学生数据导入完成: 新增 {student_count} 条 (共 {len(STUDENTS)} 条)")

        course_count = import_courses(cursor)
        conn.commit()
        print(f"[OK] 课程数据导入完成: 新增 {course_count} 条 (共 {len(COURSES)} 条)")

        print("\n" + "=" * 50)
        print(f"  导入汇总: 学生 {student_count} | 课程 {course_count}")
        print("=" * 50)

    except Exception as e:
        conn.rollback()
        print(f"\n[ERROR] 导入失败: {e}")
        sys.exit(1)
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    main()