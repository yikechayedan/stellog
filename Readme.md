# Stellog 项目说明

Stellog 是一个习惯打卡 Android 应用。当前版本主要实现了活动创建、卡片展示、今日打卡、取消打卡、记录详情数量、本周打卡圆点展示，以及主界面底部导航中的日历页面。

当前项目已经接入 Room。`Habit` 和 `CheckInRecord` 会持久化到本地 SQLite 数据库文件 `stellog.db` 中。项目按 UI 层、数据模型层、数据库层、Repository 层和工具层拆分。

## 项目结构

```text
app/src/main/java/com/example/stellog/
├── ui/
│   ├── MainActivity.java
│   ├── CreateHabitActivity.java
│   └── RecordDetailActivity.java
├── data/
│   ├── model/
│   │   ├── Habit.java
│   │   ├── CheckInRecord.java
│   │   └── CalendarDaySpec.java
│   ├── database/
│   │   ├── StellogDatabase.java
│   │   ├── HabitEntity.java
│   │   ├── CheckInRecordEntity.java
│   │   ├── HabitDao.java
│   │   └── CheckInRecordDao.java
│   └── repository/
│       └── HabitRepository.java
└── util/
    ├── DateUtils.java
    └── DimensionUtils.java
```

## 分层说明

### ui

`ui` 包负责页面展示、点击事件、页面跳转和界面刷新。

| 文件 | 作用 |
| --- | --- |
| `MainActivity.java` | 主页面，负责习惯卡片、底部导航、日历页面切换、打卡操作和记录详情入口。 |
| `CreateHabitActivity.java` | 创建活动页面，输入活动名称和单位。 |
| `RecordDetailActivity.java` | 记录详情页面，输入今天完成的数量。 |

### data.model

`data.model` 包负责保存业务层和 UI 层使用的数据结构。

| 文件 | 作用 |
| --- | --- |
| `Habit.java` | 习惯活动业务模型。 |
| `CheckInRecord.java` | 单次打卡记录业务模型。 |
| `CalendarDaySpec.java` | 日历单个日期格子的展示数据模型。 |

### data.database

`data.database` 包负责 Room 数据库交互。

| 文件 | 作用 |
| --- | --- |
| `StellogDatabase.java` | Room 数据库入口，创建并复用 `stellog.db`。 |
| `HabitEntity.java` | `habits` 表结构，并负责 `Habit` 与 Entity 的互转。 |
| `CheckInRecordEntity.java` | `check_in_records` 表结构，并负责 `CheckInRecord` 与 Entity 的互转。 |
| `HabitDao.java` | `habits` 表的数据访问接口。 |
| `CheckInRecordDao.java` | `check_in_records` 表的数据访问接口。 |

Room 当前配置：

```java
Room.databaseBuilder(
        context.getApplicationContext(),
        StellogDatabase.class,
        "stellog.db"
)
        .allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .build();
```

说明：

- `stellog.db` 是实际数据库文件名，存放在 App 私有数据目录。
- `allowMainThreadQueries()` 仅用于当前开发阶段快速跑通功能，后续应迁移到后台线程。
- `fallbackToDestructiveMigration()` 仅用于开发阶段，表结构变化且没有迁移脚本时会重建数据库，可能清空旧数据。

### data.repository

`data.repository` 包负责业务逻辑。

| 文件 | 作用 |
| --- | --- |
| `HabitRepository.java` | 对 UI 层提供稳定方法，对下通过 Room DAO 读写习惯和打卡记录。 |

Repository 的职责：

- 创建习惯。
- 查询今日打卡记录。
- 今日打卡。
- 取消今日打卡。
- 修改今日记录详情数量。
- 维护一份 `habits` 内存列表作为 `ViewPager2` 的当前数据源。

### util

`util` 包负责通用工具逻辑。

| 文件 | 作用 |
| --- | --- |
| `DateUtils.java` | 日期相关工具，包括本周日期生成、今日日期字符串、日期比较、清除时间字段。 |
| `DimensionUtils.java` | 尺寸换算工具，目前用于 `dp` 转 `px`。 |

## Room 运行机制

当前数据流大致是：

```text
MainActivity
        -> HabitRepository
        -> HabitDao / CheckInRecordDao
        -> Room
        -> SQLite 数据库文件 stellog.db
```

### Entity

Entity 表示数据库表结构。

```text
HabitEntity              -> habits 表
CheckInRecordEntity      -> check_in_records 表
```

业务层不直接使用 Entity，而是使用 `Habit` 和 `CheckInRecord`。Entity 中提供互转方法：

```java
HabitEntity.fromModel(habit)
habitEntity.toModel()

CheckInRecordEntity.fromModel(record)
checkInRecordEntity.toModel()
```

### DAO

DAO 是数据库访问接口。它只声明查询、插入、更新、删除方法，Room 会在编译期自动生成实现。

例如：

```java
@Query("SELECT * FROM habits ORDER BY id ASC")
List<HabitEntity> getAll();

@Insert
void insert(HabitEntity habit);
```

## 日历页面

日历页面目前位于 `MainActivity` 中，通过底部导航“日历”进入，点击“首页”返回主卡片页面。

当前日历能力：

- 动态生成当前显示月份的日期表。
- 支持点击左右箭头切换上一个月 / 下一个月。
- 日期表固定为 6 行 x 7 列，按周一到周日排列。
- 上月 / 下月补位日期使用浅色显示。
- `selectedDate` 保存当前选中的日期，默认是今天。
- 今天和选中日期是两个独立状态：
  - 今天：日期下方显示绿色小点。
  - 选中日期：绿色背景 + 白色数字。
- 当前尚未把日历日期与 Room 中的打卡记录绑定。

核心文件：

| 文件 | 作用 |
| --- | --- |
| `activity_main.xml` | 日历页面容器、顶部活动筛选框、月份切换栏和 `GridLayout`。 |
| `item_calendar_day.xml` | 单个日期格子的布局，包括日期数字、今天小点和数量角标。 |
| `CalendarDaySpec.java` | 单个日期格子的展示状态。 |

## 核心数据结构

### Habit

`Habit` 表示一个习惯活动，也对应主页面中的一张活动卡片。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `id` | `long` | 活动自身 id，用于和打卡记录关联。 |
| `userId` | `long` | 创建者 id，当前默认是 `0`。 |
| `name` | `String` | 活动名称。 |
| `unit` | `String` | 活动计量单位，可以为空。 |
| `recordNum` | `int` | 已打卡次数。 |
| `reminderEnabled` | `boolean` | 是否开启提醒。 |
| `sortWeight` | `int` | 排序权重。 |
| `totalValue` | `long` | 累计完成数量。 |
| `createdAt` | `long` | 创建时间戳。 |
| `updatedAt` | `long` | 更新时间戳。 |

### CheckInRecord

`CheckInRecord` 表示某个活动在某一天的一条打卡记录。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `id` | `long` | 记录自身 id。 |
| `habitId` | `long` | 绑定的活动 id。 |
| `userId` | `long` | 创建者 id。 |
| `date` | `RecordDate` | 打卡日期，只保存年月日。 |
| `value` | `long` | 用户填写的完成数量。 |
| `source` | `String` | 打卡来源。 |
| `createdAt` | `long` | 创建时间戳。 |
| `updatedAt` | `long` | 更新时间戳。 |

## 当前数据流

### 创建活动

```text
CreateHabitActivity 输入 name/unit
        -> setResult 返回 habit_name/habit_unit
        -> MainActivity.createHabitLauncher 接收
        -> MainActivity.addHabit(name, unit)
        -> HabitRepository.addHabit(name, unit)
        -> HabitDao.insert(HabitEntity)
        -> habits 内存列表新增 Habit
        -> ViewPager2 刷新并展示新卡片
```

### 今日打卡

```text
点击打卡按钮
        -> MainActivity.checkInToday(habit)
        -> HabitRepository.checkInToday(habit)
        -> CheckInRecordDao.findOnDate(...) 判断今天是否已有记录
        -> CheckInRecordDao.insert(CheckInRecordEntity)
        -> 更新 Habit 的 recordNum / totalValue / updatedAt
        -> HabitDao.update(HabitEntity)
        -> 刷新当前卡片
```

### 记录详情

```text
点击记录详情
        -> MainActivity.showRecordDetailPage(habit)
        -> RecordDetailActivity 输入完成数量
        -> setResult 返回 habit_id/record_value
        -> MainActivity.recordDetailLauncher 接收
        -> HabitRepository.applyRecordDetailValue(habitId, newValue)
        -> CheckInRecordDao.update(CheckInRecordEntity)
        -> HabitDao.update(HabitEntity)
        -> 刷新对应卡片
```

### 取消今日打卡

```text
点击取消
        -> MainActivity.cancelTodayCheckIn(habit)
        -> HabitRepository.cancelTodayCheckIn(habit)
        -> CheckInRecordDao.delete(CheckInRecordEntity)
        -> 更新 Habit 的 recordNum / totalValue / updatedAt
        -> HabitDao.update(HabitEntity)
        -> 刷新当前卡片
```
