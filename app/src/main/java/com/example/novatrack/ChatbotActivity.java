package com.example.novatrack;

import android.app.ProgressDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ChatbotActivity extends AppCompatActivity {

    private LinearLayout layoutMessages;
    private EditText etMessage;
    private Button btnSend;
    private ScrollView scrollView;
    private ImageButton btnAttach;
    private ProgressDialog dialog;

    private Uri selectedFileUri;
    private final String API_KEY = "AIzaSyBXhqGygsVsMR8c8zl1uuLhrlJ8JtjlAt0";

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUri = result.getData().getData();
                    if (selectedFileUri != null) {
                        String fileName = getFileName(selectedFileUri);
                        addMessage("📎 Attached: " + fileName, true);

                        // Copy file and upload
                        try {
                            File file = copyUriToFile(selectedFileUri);
                            uploadFileToGemini(file);
                        } catch (Exception e) {
                            addMessage("File error: " + e.getMessage(), false);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot_actvity);

        layoutMessages = findViewById(R.id.layoutMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        scrollView = findViewById(R.id.scrollView);
        btnAttach = findViewById(R.id.btnAttach);
        dialog = new ProgressDialog(this);

        btnAttach.setOnClickListener(v -> openFilePicker());

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                addMessage(msg, true);
                etMessage.setText("");
                getGPTResponse(msg);
            }
        });

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                String msg = etMessage.getText().toString().trim();
                if (!msg.isEmpty()) {
                    addMessage(msg, true);
                    etMessage.setText("");
                    getGPTResponse(msg);
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

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"));
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private File copyUriToFile(Uri uri) throws Exception {
        String name = getFileName(uri);
        File tempFile = new File(getCacheDir(), name);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
        return tempFile;
    }

    private void uploadFileToGemini(File file) {
        dialog.setMessage("Uploading file...");
        dialog.show();

        String url = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + API_KEY;

        VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, url,
                response -> {
                    dialog.dismiss();
                    try {
                        JSONObject obj = new JSONObject(new String(response.data));
                        String fileName = obj.getJSONObject("file").getString("name");
                        addMessage("✅ File uploaded", false);
                        askGeminiWithFile(fileName);
                    } catch (Exception e) {
                        addMessage(e.getMessage(), false);
                    }
                },
                error -> {
                    dialog.dismiss();
                    addMessage("Upload failed: " + error.getMessage(), false);
                }) {
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] bytes = new byte[(int) file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    fis.read(bytes);
                    fis.close();
                    params.put("file", new DataPart(file.getName(), bytes, "application/pdf"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void askGeminiWithFile(String fileName) {
        dialog.setMessage("Analyzing file...");
        dialog.show();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

        try {
            JSONObject textPart = new JSONObject();
            textPart.put("text", "Break this PDF into actionable tasks.");

            JSONObject fileData = new JSONObject();
            fileData.put("file_uri", fileName);

            JSONObject filePart = new JSONObject();
            filePart.put("file_data", fileData);

            JSONArray parts = new JSONArray();
            parts.put(textPart);
            parts.put(filePart);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        dialog.dismiss();
                        try {
                            String result = response.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            addMessage(result, false);
                            sendNotification("Tasks extracted from your file!");
                        } catch (Exception e) {
                            addMessage(e.getMessage(), false);
                        }
                    },
                    error -> {
                        dialog.dismiss();
                        addMessage("Error: " + error.getMessage(), false);
                    });

            Volley.newRequestQueue(this).add(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "chatbot_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Chatbot", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("NovaTrack")
                .setContentText(text);

        manager.notify(1, builder.build());
    }

    private void getGPTResponse(String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(this);
        dialog.setMessage("Processing...");
        dialog.show();
        try {
            JSONObject text = new JSONObject();
            text.put("text", userMessage);
            JSONArray parts = new JSONArray();
            parts.put(text);
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        dialog.dismiss();
                        try {
                            String result = response.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");
                            addMessage(result, false);
                        } catch (Exception e) {
                            addMessage(e.getMessage(), false);
                        }
                    },
                    error -> {
                        dialog.dismiss();
                        addMessage("Error: " + error.getMessage(), false);
                    });

            queue.add(request);

        } catch (Exception e) {
            dialog.dismiss();
            addMessage(e.getMessage(), false);
        }
    }
}
