package com.example.ttv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import opennlp.tools.util.Span;

/**
 * Created by DK on 7/13/2017.
 */

public class SentencesDialogFragment extends DialogFragment {
    private static boolean loading = false;

    private static final String TAG = SentencesDialogFragment.class.getName();
    int count = 0;
    Timer timer;
    TextView mTitle;
    public ArrayList<Span> spanArrayList;
    ListView listView;
    ProgressBar progressBar;
    private AnswerAdapter mAnswerAdapter;
    private AlertDialog alertDialog;
    String mParagraph = "";
    TextView calTimeTv;
    long consumeTime = 0;


    public ArrayList<ArrayList<String>> posDetector;
    public ArrayList<String> jsonData;


    public SentencesDialogFragment() {
    }

    public SentencesDialogFragment(ArrayList<Span> spanArrayList, ArrayList<ArrayList<String>> posDetector, ArrayList<String> jsonData, String inputText, long time) {
        this.spanArrayList = spanArrayList;
        consumeTime = time;
        mParagraph = inputText;

        this.posDetector = posDetector;
        this.jsonData = jsonData;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_answer_dialog, container);
        mTitle = (TextView) view.findViewById(R.id.answer_title);
        listView = (ListView) view.findViewById(R.id.expandable_listview);
        progressBar = (ProgressBar) view.findViewById(R.id.loading_empty_pb);
        calTimeTv = (TextView) view.findViewById(R.id.cal_point);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        // Fetch arguments from bundle and set title

        mAnswerAdapter = new AnswerAdapter(getActivity(), spanArrayList, posDetector, jsonData);
        listView.setAdapter(mAnswerAdapter);
        progressBar.setVisibility(View.INVISIBLE);
        //listView.setEmptyView(progressBar);


        calTimeTv.setText("Sentences count: " + spanArrayList.size() +  " - Time: " + consumeTime);

    }

    public class AnswerAdapter extends BaseAdapter {

        ArrayList<Span> dataLists;

        ArrayList<ArrayList<String>> dataListDetector;
        Context context;
        ArrayList<String> jsonData;

        public AnswerAdapter(Context context, ArrayList<Span> dataList, ArrayList<ArrayList<String>> dataListDetector, ArrayList<String> jsonData) {
            this.dataLists = dataList;
            this.context = context;

            this.dataListDetector = dataListDetector;
            this.jsonData = jsonData;
        }

        @Override
        public int getCount() {
            return dataLists.size();
        }

        @Override
        public Object getItem(int position) {
            return dataLists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            Span span = (Span) getItem(position);
            if (convertView == null) {
                view = inflater.inflate(R.layout.sentence_list_item_layout, null);
            } else {
                view = convertView;
            }

            TextView sentenceTv = (TextView) view.findViewById(R.id.sentence);
            sentenceTv.setText(span.toString() + "|: " + span.getCoveredText(mParagraph) + "\n\nDetection: " + dataListDetector.get(position));

            sentenceTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progressBar.setVisibility(View.VISIBLE);
                    timer = new Timer();
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            count++;
                            progressBar.setProgress(count);
                            if (loading == true)
                            {
                                loading = false;
                                timer.cancel();
                            }
                        }
                    };
                    timer.schedule(timerTask, 0, 100);
//                    ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
//                    ClipData clipData = ClipData.newPlainText("Data", dataListDetector.get(position).toString());
//                    clipboardManager.setPrimaryClip(clipData);
//                    Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show();

                    // Dữ liệu JSON cần gửi
//                    String jsonData = "{\"objects_list\": [{\"name\": \"dog\", \"action\": \"move up\", \"timing\": \"first\"}, {\"name\": \"cat\", \"action\": \"jump\", \"timing\": \"then\"}, {\"name\": \"apple\", \"action\": \"roll\", \"timing\": \"final\"}]}";
                    //String serverUrl = "http://192.168.0.105:5000/api/json";
                    String serverUrl = "http://192.168.0.106:5000/get_video";
                    OkHttpClient client = new OkHttpClient.Builder()
                            .readTimeout(15, TimeUnit.MINUTES)
                            .build();

                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonData.get(position));

                    Request request = new Request.Builder()
                            .url(serverUrl)
                            .post(requestBody)
                            .addHeader("Content-Type", "application/json")
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            Log.d("video_path", "error");
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (response.isSuccessful()) {
                                loading = true;
                                byte[] videoContent = response.body().bytes();
                                saveVideoToFile(videoContent);
                                String videoPath = context.getFilesDir().getPath() + "/video.mp4";
                                Log.d("video_path", videoPath);
                                Intent intent = new Intent(context, DisplayActivity.class);
                                intent.putExtra("videoPath", videoPath);
                                intent.putExtra("inputText", span.getCoveredText(mParagraph));
                                startActivity(intent);
                            }
                        }
                    });

//                    Intent intent = new Intent(context, DisplayActivity.class);
//                    intent.putExtra("dataListDetector", dataListDetector.get(position));
//                    startActivity(intent);

//                    Intent intent = new Intent(context, DisplayBack.class);
//                    intent.putExtra("dataListDetector", dataListDetector.get(position));
//                    startActivity(intent);
                }
            });

            return view;
        }
    }

    private void saveVideoToFile(byte[] videoContent) {
        try {
            FileOutputStream outputStream = getContext().openFileOutput("video.mp4", Context.MODE_PRIVATE);
            outputStream.write(videoContent);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}