package com.example.stellog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreateHabitActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_habit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_habit_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.create_close_button).setOnClickListener(v -> finish());
        findViewById(R.id.create_cancel_button).setOnClickListener(v -> finish());
        findViewById(R.id.create_save_button).setOnClickListener(v -> {
            EditText nameInput = findViewById(R.id.habit_name_input);
            EditText unitInput = findViewById(R.id.habit_unit_input);

            // 获取用户输入，并去掉前后空格
            String name = nameInput.getText().toString().trim();
            String unit = unitInput.getText().toString().trim();

            // 名称不能为空
            if (name.isEmpty()) {
                nameInput.setError("活动名称不能为空");
                return;
            }

            // 单位允许为空；如果没填，就保存为空字符串
            Intent resultIntent = new Intent();
            resultIntent.putExtra("habit_name", name);
            resultIntent.putExtra("habit_unit", unit);

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
