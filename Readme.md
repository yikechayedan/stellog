# Stellog 数据与函数说明

本文档记录项目中当前较重要的数据结构、页面间传参格式，以及核心函数的输入、输出和作用。当前项目数据主要保存在内存中，尚未接入数据库。

## 数据结构

### Habit

文件：`app/src/main/java/com/example/stellog/Habit.java`

`Habit` 表示一个习惯活动，也就是主页面中的一张活动卡片。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `id` | `long` | 活动自身 id，用于和打卡记录关联。 |
| `userId` | `long` | 创建者 id。当前默认是 `0`。 |
| `name` | `String` | 活动名称，例如 `run`、`看书`。 |
| `unit` | `String` | 活动计量单位，例如 `km`、`分钟`。允许为空字符串。 |
| `recordNum` | `int` | 已打卡次数。每次今日打卡加 1，取消今日打卡减 1。 |
| `reminderEnabled` | `boolean` | 是否开启提醒。当前创建时默认为 `false`。 |
| `sortWeight` | `int` | 排序权重。当前创建时默认为 `1`。 |
| `totalValue` | `long` | 累计完成数量，由记录详细中的 `record.value` 累加得到。 |
| `createdAt` | `long` | 创建时间，格式为 `System.currentTimeMillis()` 的毫秒时间戳。 |
| `updatedAt` | `long` | 更新时间，打卡、取消打卡或修改记录详细时更新。 |

构造函数输入格式：

```java
new Habit(
        long id,
        long userId,
        String name,
        String unit,
        int recordNum,
        boolean reminderEnabled,
        int sortWeight,
        long totalValue,
        long createdAt,
        long updatedAt
)
```

输出：返回一个 `Habit` 对象。

### CheckInRecord

文件：`app/src/main/java/com/example/stellog/CheckInRecord.java`

`CheckInRecord` 表示某个活动在某一天的一条打卡记录。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `id` | `long` | 记录自身 id。 |
| `habitId` | `long` | 绑定的活动 id，对应 `Habit.id`。 |
| `userId` | `long` | 创建者 id，来自对应 habit。 |
| `date` | `RecordDate` | 打卡日期，只保存年月日。 |
| `value` | `long` | 用户在“记录详细”中填写的完成数量。普通打卡时默认是 `0`。 |
| `source` | `String` | 打卡来源，例如 `正常打卡`、`补打卡`。 |
| `createdAt` | `long` | 创建时间，毫秒时间戳。 |
| `updatedAt` | `long` | 更新时间，修改记录详细时更新。 |

构造函数输入格式：

```java
new CheckInRecord(
        long id,
        long habitId,
        long userId,
        CheckInRecord.RecordDate date,
        long value,
        String source,
        long createdAt,
        long updatedAt
)
```

输出：返回一个 `CheckInRecord` 对象。

### RecordDate

文件：`app/src/main/java/com/example/stellog/CheckInRecord.java`

`RecordDate` 是 `CheckInRecord` 的内部类，用于只记录年月日，避免直接比较毫秒时间戳时受到时分秒影响。

| 字段 | 类型 | 作用 |
| --- | --- | --- |
| `year` | `int` | 年，例如 `2026`。 |
| `month` | `int` | 月，范围是 `1-12`。 |
| `day` | `int` | 日，范围通常是 `1-31`。 |

重要函数：

```java
boolean isSameDay(RecordDate other)
```

输入：另一个 `RecordDate`。  
输出：`boolean`。年月日完全相同返回 `true`，否则返回 `false`。

```java
static RecordDate today()
```

输入：无。  
输出：本地系统今天对应的 `RecordDate`。

```java
static RecordDate fromCalendar(Calendar calendar)
```

输入：Java 的 `Calendar` 对象。  
输出：转换后的 `RecordDate`，其中月份会从 `Calendar` 的 `0-11` 转为正常的 `1-12`。

## 页面间传参格式

### 创建活动页面返回 MainActivity

发送方：`CreateHabitActivity`  
接收方：`MainActivity.createHabitLauncher`

返回结果：

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_name` | `String` | 新活动名称，不能为空。 |
| `habit_unit` | `String` | 新活动单位，可以为空。 |

保存时返回格式：

```java
Intent resultIntent = new Intent();
resultIntent.putExtra("habit_name", name);
resultIntent.putExtra("habit_unit", unit);
setResult(RESULT_OK, resultIntent);
finish();
```

### 记录详细页面输入参数

发送方：`MainActivity.showRecordDetailPage()`  
接收方：`RecordDetailActivity`

输入参数：

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_id` | `long` | 当前活动 id。 |
| `habit_name` | `String` | 当前活动名称，用于页面展示。 |
| `habit_unit` | `String` | 当前活动单位，用于页面展示。 |
| `record_value` | `long` | 今天这条打卡记录已有的完成数量。 |

### 记录详细页面返回 MainActivity

发送方：`RecordDetailActivity.saveRecordValue()`  
接收方：`MainActivity.recordDetailLauncher`

返回结果：

| key | 类型 | 作用 |
| --- | --- | --- |
| `habit_id` | `long` | 要更新的活动 id。 |
| `record_value` | `long` | 用户填写的新完成数量。 |

保存时返回格式：

```java
Intent resultIntent = new Intent();
resultIntent.putExtra("habit_id", habitId);
resultIntent.putExtra("record_value", value);
setResult(RESULT_OK, resultIntent);
finish();
```

## MainActivity 核心数据

文件：`app/src/main/java/com/example/stellog/MainActivity.java`

| 变量 | 类型 | 作用 |
| --- | --- | --- |
| `DEFAULT_CHECK_IN_VALUE` | `long` | 普通打卡时 `CheckInRecord.value` 的默认值，目前是 `0L`。 |
| `habits` | `List<Habit>` | 内存中的活动列表。 |
| `records` | `List<CheckInRecord>` | 内存中的打卡记录列表。 |
| `habitPager` | `ViewPager2` | 主页面卡片滑动组件。 |
| `habitAdapter` | `HabitPagerAdapter` | 将 `habits` 渲染为卡片页面的适配器。 |
| `pageIndicatorText` | `TextView` | 右上角页码文本。 |
| `currentHabitPosition` | `int` | 当前展示的卡片序号。 |

## MainActivity 核心函数

### setupHabitPager

```java
private void setupHabitPager()
```

输入：无。  
输出：无。  
作用：初始化 `ViewPager2`，绑定 `HabitPagerAdapter`，设置预渲染、左右 padding、卡片缩放/透明度变换，并监听当前页变化。

### addHabit

```java
private void addHabit(String name, String unit)
```

输入：

| 参数 | 类型 | 作用 |
| --- | --- | --- |
| `name` | `String` | 活动名称。 |
| `unit` | `String` | 活动单位。 |

输出：无。  
作用：根据创建活动页面返回的数据生成一个新的 `Habit`，加入 `habits`，刷新卡片列表，并滑动到新活动卡片。

默认值：

| 字段 | 默认值 |
| --- | --- |
| `userId` | `0` |
| `recordNum` | `0` |
| `reminderEnabled` | `false` |
| `sortWeight` | `1` |
| `totalValue` | `0` |
| `createdAt` / `updatedAt` | 当前系统毫秒时间戳 |

### updateHeader

```java
private void updateHeader(int position)
```

输入：当前卡片位置 `position`，从 `0` 开始。  
输出：无。  
作用：更新右上角页码，例如第 1 张卡片显示为 `1 / 3`。

### getTodayRecord

```java
private CheckInRecord getTodayRecord(long habitId)
```

输入：活动 id。  
输出：如果该活动今天已经打卡，返回今天的 `CheckInRecord`；否则返回 `null`。  
作用：判断当前活动今天是否存在打卡记录。

### checkInToday

```java
private void checkInToday(Habit habit)
```

输入：要打卡的 `Habit`。  
输出：无。  
作用：如果该活动今天还没有打卡，则创建一条新的 `CheckInRecord`，加入 `records`，并更新：

| 更新对象 | 更新内容 |
| --- | --- |
| `records` | 新增今日 record。 |
| `habit.recordNum` | 加 1。 |
| `habit.totalValue` | 加上 `record.value`，当前默认加 `0`。 |
| `habit.updatedAt` | 更新为当前毫秒时间戳。 |
| UI | 刷新当前卡片。 |

### cancelTodayCheckIn

```java
private void cancelTodayCheckIn(Habit habit)
```

输入：要取消今日打卡的 `Habit`。  
输出：无。  
作用：如果该活动今天有打卡记录，则删除今天的 `CheckInRecord`，并更新：

| 更新对象 | 更新内容 |
| --- | --- |
| `records` | 删除今日 record。 |
| `habit.recordNum` | 减 1，最低为 0。 |
| `habit.totalValue` | 减去今日 record 的 `value`，最低为 0。 |
| `habit.updatedAt` | 更新为当前毫秒时间戳。 |
| UI | 刷新当前卡片。 |

### generateRecordId

```java
private long generateRecordId()
```

输入：无。  
输出：新的 record id。  
作用：如果 `records` 为空返回 `1`，否则返回最后一条记录 id 加 1。

### hasRecordOnDate

```java
private boolean hasRecordOnDate(long habitId, CheckInRecord.RecordDate date)
```

输入：活动 id 和指定日期。  
输出：该活动在指定日期存在打卡记录返回 `true`，否则返回 `false`。  
作用：用于渲染本周 7 个打卡圆点。

### getCurrentWeekDates

```java
private List<CheckInRecord.RecordDate> getCurrentWeekDates()
```

输入：无。  
输出：本周周一到周日的 7 个 `RecordDate`。  
作用：生成本周日期列表，供卡片上的星期圆点绑定打卡状态。

### getTodayDateString

```java
private String getTodayDateString()
```

输入：无。  
输出：今日日期字符串，格式为 `yyyy-MM-dd`。  
作用：卡片中显示 `今天 | yyyy-MM-dd`。

### showRecordDetailPage

```java
private void showRecordDetailPage(Habit habit)
```

输入：当前活动 `Habit`。  
输出：无。  
作用：如果当前活动今天已经打卡，则打开 `RecordDetailActivity`，并传入活动 id、名称、单位和当前今日 record 的 value。如果今天未打卡，则提示先完成今日打卡。

### applyRecordDetailValue

```java
private void applyRecordDetailValue(long habitId, long newValue)
```

输入：

| 参数 | 类型 | 作用 |
| --- | --- | --- |
| `habitId` | `long` | 要更新的活动 id。 |
| `newValue` | `long` | 记录详细页面返回的新完成数量。 |

输出：无。  
作用：找到对应活动和今天的 record，用新值更新 `record.value`，并按差值更新 `habit.totalValue`。

计算方式：

```java
habit.totalValue = habit.totalValue - oldValue + newValue;
```

这样可以避免用户第二次修改记录详细时重复累计。

### findHabitPosition

```java
private int findHabitPosition(long habitId)
```

输入：活动 id。  
输出：活动在 `habits` 列表中的位置；如果找不到返回 `-1`。  
作用：用于根据 `habitId` 找到要刷新的卡片位置。

## Adapter 与卡片绑定

### HabitPagerAdapter

`HabitPagerAdapter` 继承自 `RecyclerView.Adapter`，负责将 `List<Habit>` 渲染成 `ViewPager2` 中的一张张卡片。

关键函数：

```java
onCreateViewHolder(...)
```

作用：加载 `item_habit_card.xml`，创建卡片 ViewHolder。

```java
onBindViewHolder(...)
```

作用：把第 `position` 个 `Habit` 绑定到卡片控件上。

```java
getItemCount()
```

作用：返回活动数量，也就是卡片数量。

### HabitViewHolder.bind

```java
void bind(Habit habit)
```

输入：当前要展示的 `Habit`。  
输出：无。  
作用：把活动数据和今日打卡状态绑定到卡片 UI，包括：

| UI 内容 | 数据来源 |
| --- | --- |
| 活动名称 | `habit.name` |
| 多少天收获 | `habit.recordNum` |
| 此日计 | 今日 `CheckInRecord.value` |
| 已累计 | `habit.totalValue` |
| 今日日期 | `getTodayDateString()` |
| 本周圆点 | `bindWeekDots(habit.id)` |
| 打卡按钮/已打卡操作区 | 今天是否存在 `CheckInRecord` |

### bindWeekDots

```java
private void bindWeekDots(long habitId)
```

输入：活动 id。  
输出：无。  
作用：先获取本周周一到周日的日期，再逐个判断这些日期是否有打卡记录。有记录则显示绿色圆点和对勾，否则显示空心圆。

## CreateHabitActivity 核心函数

### saveHabitInput

```java
private void saveHabitInput()
```

输入：无，直接读取页面中的两个 `EditText`。  
输出：通过 `setResult(RESULT_OK, resultIntent)` 返回 `habit_name` 和 `habit_unit`。  
作用：校验活动名称不能为空，单位可以为空；校验通过后把输入结果返回给 `MainActivity`。

## RecordDetailActivity 核心函数

### saveRecordValue

```java
private void saveRecordValue()
```

输入：无，直接读取页面中的 `record_value_input`。  
输出：通过 `setResult(RESULT_OK, resultIntent)` 返回 `habit_id` 和 `record_value`。  
作用：校验完成数量不能为空、必须是有效数字、不能小于 0；校验通过后返回给 `MainActivity` 更新 record 和 habit。

## 当前数据流概览

### 创建活动

```text
CreateHabitActivity 输入 name/unit
        ↓
setResult 返回 habit_name/habit_unit
        ↓
MainActivity.createHabitLauncher 接收
        ↓
addHabit(name, unit)
        ↓
habits 新增 Habit
        ↓
ViewPager2 刷新并展示新卡片
```

### 今日打卡

```text
点击打卡按钮
        ↓
checkInToday(habit)
        ↓
records 新增 CheckInRecord(value = 0)
        ↓
habit.recordNum + 1
        ↓
刷新当前卡片
```

### 记录详细

```text
点击记录详细
        ↓
showRecordDetailPage(habit)
        ↓
RecordDetailActivity 输入完成数量
        ↓
setResult 返回 habit_id/record_value
        ↓
MainActivity.recordDetailLauncher 接收
        ↓
applyRecordDetailValue(habitId, newValue)
        ↓
更新 todayRecord.value 和 habit.totalValue
        ↓
刷新对应卡片
```

### 取消今日打卡

```text
点击取消
        ↓
cancelTodayCheckIn(habit)
        ↓
删除今天的 CheckInRecord
        ↓
habit.recordNum - 1
habit.totalValue - todayRecord.value
        ↓
刷新当前卡片
```
