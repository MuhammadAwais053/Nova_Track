package com.example.novatrack;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.util.Map;

public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private static final String TWO_HYPHENS = "--";
    private static final String LINE_END = "\r\n";
    private static final String BOUNDARY = "volley-multipart-boundary";

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
    }

    // Inner class for sending file data
    public static class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + BOUNDARY;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            Map<String, DataPart> params = getByteData();
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, DataPart> entry : params.entrySet()) {
                    buildPart(dos, entry.getKey(), entry.getValue());
                }
            }
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }

    private void buildPart(DataOutputStream dos, String key, DataPart dataFile) throws IOException {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + LINE_END);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + dataFile.getFileName() + "\"" + LINE_END);
        dos.writeBytes("Content-Type: " + dataFile.getType() + LINE_END);
        dos.writeBytes(LINE_END);

        dos.write(dataFile.getContent());

        dos.writeBytes(LINE_END);
    }

    protected abstract Map<String, DataPart> getByteData() throws AuthFailureError;
}
