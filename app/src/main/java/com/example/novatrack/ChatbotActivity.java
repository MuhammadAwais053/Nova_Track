package com.example.novatrack;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class ChatbotActivity extends AppCompatActivity {

    LinearLayout layoutMessages;
    EditText etMessage;
    Button btnSend;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_actvity);

        layoutMessages = findViewById(R.id.layoutMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        scrollView = findViewById(R.id.scrollView);

        String firstMessage = getIntent().getStringExtra("USER_MESSAGE");
        if (firstMessage != null && !firstMessage.isEmpty()) {
            addMessage(firstMessage, true);
            getGPTResponse(firstMessage);
        }

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessage(message, true);
                etMessage.setText("");
                getGPTResponse(message);
            }
        });

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND
                    || actionId == EditorInfo.IME_ACTION_DONE) {

                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    addMessage(message, true);
                    etMessage.setText("");
                    getGPTResponse(message);
                }
                return true;
            }
            return false;
        });
    }

    private void addMessage(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setPadding(20, 12, 20, 12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        if (isUser) {
            textView.setBackgroundResource(R.drawable.user_msg);
            textView.setTextColor(Color.WHITE);
            params.gravity = Gravity.END;
            params.setMargins(50, 10, 10, 10);
        } else {
            textView.setBackgroundResource(R.drawable.bot_msg);
            textView.setTextColor(Color.BLACK);
            params.gravity = Gravity.START;
            params.setMargins(10, 10, 50, 10);
        }

        textView.setLayoutParams(params);
        layoutMessages.addView(textView);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void getGPTResponse(String userMessage) {

        String url = "https://192.168.100.198/chat";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", userMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    try {
                        String botText = response.getString("reply");
                        addMessage(botText, false);
                    } catch (Exception e) {
                        addMessage("Invalid response from bot.", false);
                    }
                },
                error -> {
                    Log.e("VOLLEY_ERROR", error.toString());
                    addMessage("Bot is not responding 😕", false);
                }
        );

        VolleySingleton.getInstance(this)
                .getRequestQueue()
                .add(request);
    }
}
