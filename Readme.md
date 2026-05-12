# Stellog 项目说明

Stellog 是一个习惯打卡 Android 应用。当前版本主要实现了活动创建、卡片展示、今日打卡、取消打卡、记录详细数量、本周打卡圆点展示，以及主界面底部导航中的日历页面（未绑定打卡记录）。

当前项目还没有接入数据库，数据仍然保存在内存中。项目已按 UI 层、数据模型层、Repository 数据操作层和工具层进行拆分，方便后续接入 Room / SQLite。

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

`data.model` 包负责保存核心数据结构。

| 文件 | 作用 |
| --- | --- |
| `Habit.java` | 习惯活动数据模型。 |
| `CheckInRecord.java` | 单次打卡记录数据模型。 |
| `CalendarDaySpec.java` | 日历单个日期格子的展示数据模型。 |

### data.repository

`data.repository` 包负责数据操作和业务逻辑。

| 文件 | 作用 |
| --- | --- |
| `HabitRepository.java` | 管理内存中的活动列表和打卡记录列表，提供创建活动、打卡、取消打卡、更新记录详情等方法。 |

目前 Repository 内部仍然使用：

```java
private final List<Habit> habits = new ArrayList<>();
private final List<CheckInRecord> records = new ArrayList<>();
```

以后接入 Room / SQLite 时，优先替换 Repository 内部实现，UI 层尽量少改。

### util

`util` 包负责通用工具逻辑。

| 文件 | 作用 |
| --- | --- |
| `DateUtils.java` | 日期相关工具，包括本周日期生成、今日日期字符串、日期比较、清除时间字段。 |
| `DimensionUtils.java` | 尺寸换算工具，目前用于 `dp` 转 `px`。 |

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
  - 选中日期：保持绿色背景 + 白色数字。
- 当前尚未接入日历中的真实打卡状态和多活动数量角标，相关 UI 结构已保留在 `item_calendar_day.xml` 中。

核心文件：

| 文件 | 作用 |
| --- | --- |
| `activity_main.xml` | 日历页面容器、顶部活动筛选框、月份切换栏和 `GridLayout`。 |
| `item_calendar_day.xml` | 单个日期格子的布局，包括日期数字、今天小点和数量角标。 |
| `CalendarDaySpec.java` | 单个日期格子的展示状态。 |

## 核心数据结构

### Habit

文件：`app/src/main/java/com/example/stellog/data/model/Habit.java`

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

文件：`app/src/main/java/com/example/stellog/data/model/CheckInRecord.java`

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

### CalendarDaySpec

文件：`app/src/main/java/com/example/stellog/data/model/CalendarDaySpec.java`

`CalendarDaySpec` 表示日历中一个日期格子的展示状态。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `date` | `Calendar` | 日期对象。 |
| `label` | `String` | 显示在格子里的日期数字。 |
| `today` | `boolean` | 是否是今天。 |
| `selected` | `boolean` | 是否是当前选中日期。 |
| `outsideMonth` | `boolean` | 是否是不属于当前显示月份的补位日期。 |

## MainActivity 说明

文件：`app/src/main/java/com/example/stellog/ui/MainActivity.java`

`MainActivity` 目前负责：

- 初始化习惯卡片 `ViewPager2`。
- 处理创建活动、打卡、取消打卡和记录详情。
- 切换首页 / 日历页。
- 渲染日历月份标题和日期表。
- 维护当前显示月份 `visibleMonth`。
- 维护当前选中日期 `selectedDate`。

日历相关核心方法：

| 方法 | 作用 |
| --- | --- |
| `setupCalendarNavigation()` | 初始化左右月份切换按钮。 |
| `renderCalendarGrid()` | 根据当前月份重新渲染 42 个日期格。 |
| `buildVisibleMonthDays()` | 根据 `visibleMonth` 生成 6 x 7 的日期数据。 |
| `bindCalendarDay(...)` | 将 `CalendarDaySpec` 绑定到单个日期 item。 |

## DateUtils 说明

文件：`app/src/main/java/com/example/stellog/util/DateUtils.java`

| 方法 | 作用 |
| --- | --- |
| `getCurrentWeekDates()` | 返回本周周一到周日的 7 个 `RecordDate`。 |
| `getTodayDateString()` | 返回今日日期字符串，格式为 `yyyy-MM-dd`。 |
| `isSameDate(Calendar left, Calendar right)` | 判断两个 `Calendar` 是否是同一天。 |
| `clearTime(Calendar calendar)` | 将时分秒毫秒清零，只保留年月日语义。 |

## DimensionUtils 说明

文件：`app/src/main/java/com/example/stellog/util/DimensionUtils.java`

| 方法 | 作用 |
| --- | --- |
| `dpToPx(Resources resources, int dp)` | 将 dp 转换为 px。 |

## 页面传参

### 创建活动页面返回 MainActivity

发送方：`CreateHabitActivity`  
接收方：`MainActivity.createHabitLauncher`

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_name` | `String` | 新活动名称，不能为空。 |
| `habit_unit` | `String` | 新活动单位，可以为空。 |

### 记录详情页面输入参数

发送方：`MainActivity.showRecordDetailPage()`  
接收方：`RecordDetailActivity`

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_id` | `long` | 当前活动 id。 |
| `habit_name` | `String` | 当前活动名称。 |
| `habit_unit` | `String` | 当前活动单位。 |
| `record_value` | `long` | 今日记录已有的完成数量。 |

### 记录详情页面返回 MainActivity

发送方：`RecordDetailActivity.saveRecordValue()`  
接收方：`MainActivity.recordDetailLauncher`

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_id` | `long` | 要更新的活动 id。 |
| `record_value` | `long` | 用户填写的新完成数量。 |

## 当前数据流

### 创建活动

```text
CreateHabitActivity 输入 name/unit
        -> setResult 返回 habit_name/habit_unit
        -> MainActivity.createHabitLauncher 接收
        -> MainActivity.addHabit(name, unit)
        -> HabitRepository.addHabit(name, unit)
        -> habits 新增 Habit
        -> ViewPager2 刷新并展示新卡片
```

### 今日打卡

```text
点击打卡按钮
        -> MainActivity.checkInToday(habit)
        -> HabitRepository.checkInToday(habit)
        -> records 新增 CheckInRecord(value = 0)
        -> habit.recordNum + 1
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
        -> 更新 todayRecord.value 和 habit.totalValue
        -> 刷新对应卡片
```

### 取消今日打卡

```text
点击取消
        -> MainActivity.cancelTodayCheckIn(habit)
        -> HabitRepository.cancelTodayCheckIn(habit)
        -> 删除今天的 CheckInRecord
        -> habit.recordNum - 1
        -> habit.totalValue - todayRecord.value
        -> 刷新当前卡片
```
