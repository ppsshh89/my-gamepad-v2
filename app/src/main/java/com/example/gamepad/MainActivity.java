package com.example.gamepad;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends Activity {
    private DatagramSocket udpSocket;
    private int serverPort = 5005;

    private FrameLayout gameLayout;
    private boolean isEditMode = false;
    private View selectedButton = null;
    private float dX, dY;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.TRANSPARENT);

        gameLayout = new FrameLayout(this);
        rootLayout.addView(gameLayout, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(20, 20, 20, 20);

        Button btnEdit = new Button(this);
        btnEdit.setText("⚙️ 편집/잠금");
        topBar.addView(btnEdit);

        Button btnAdd = new Button(this);
        btnAdd.setText("➕ 버튼추가");
        topBar.addView(btnAdd);

        SeekBar seekBarOpacity = new SeekBar(this);
        seekBarOpacity.setMax(100);
        seekBarOpacity.setProgress(60);
        seekBarOpacity.setLayoutParams(new LinearLayout.LayoutParams(300, -2));
        topBar.addView(seekBarOpacity);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(" IP:5005 ");
        tvInfo.setTextColor(Color.WHITE);
        topBar.addView(tvInfo);

        rootLayout.addView(topBar, new FrameLayout.LayoutParams(-2, -2));
        setContentView(rootLayout);

        new Thread(() -> {
            try {
                udpSocket = new DatagramSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = (float) progress / 100f;
                for (int i = 0; i < gameLayout.getChildCount(); i++) {
                    gameLayout.getChildAt(i).setAlpha(alpha);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnEdit.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            btnEdit.setText(isEditMode ? "🔒 잠금(게임)" : "⚙️ 편집/잠금");
            selectedButton = null;
        });

        btnAdd.setOnClickListener(v -> addGamepadButton("RT+A", 300, 300, 200, 120));

        addGamepadButton("UP", 150, 400, 150, 150);
        addGamepadButton("DOWN", 150, 700, 150, 150);
        addGamepadButton("LEFT", 50, 550, 150, 150);
        addGamepadButton("RIGHT", 250, 550, 150, 150);

        addGamepadButton("A", 1600, 600, 160, 160);
        addGamepadButton("B", 1800, 450, 160, 160);
        addGamepadButton("RT", 1600, 200, 200, 120);
        addGamepadButton("LB+RT", 1350, 200, 200, 120);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addGamepadButton(String cmdName, int x, int y, int w, int h) {
        Button btn = new Button(this);
        btn.setText(cmdName);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#44FFFFFF"));
        btn.setAlpha(0.6f);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(w, h);
        params.leftMargin = x;
        params.topMargin = y;
        gameLayout.addView(btn, params);

        btn.setOnTouchListener((v, event) -> {
            if (isEditMode) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        selectedButton = v;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (selectedButton == v) {
                            v.animate()
                                    .x(event.getRawX() + dX)
                                    .y(event.getRawY() + dY)
                                    .setDuration(0)
                                    .start();
                        }
                        return true;
                }
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setPressed(true);
                        sendUdp("COMBO:" + cmdName + ":DOWN");
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setPressed(false);
                        sendUdp("COMBO:" + cmdName + ":UP");
                        return true;
                }
            }
            return false;
        });
    }

    private void sendUdp(String msg) {
        new Thread(() -> {
            try {
                if (udpSocket == null) return;
                InetAddress addr = InetAddress.getByName("192.168.0.10"); 
                byte[] data = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, addr, serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
