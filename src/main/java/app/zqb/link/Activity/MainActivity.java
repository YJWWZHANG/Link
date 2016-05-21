package app.zqb.link.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Timer;
import java.util.TimerTask;

import app.zqb.link.CustomView.GameView;
import app.zqb.link.CustomView.Piece;
import app.zqb.link.Game.GameConf;
import app.zqb.link.Game.GameService;
import app.zqb.link.Game.GameServiceImpl;
import app.zqb.link.Game.LinkInfo;
import app.zqb.link.R;
import app.zqb.link.Utils.L;
import app.zqb.link.Utils.MyApplication;

import static app.zqb.link.Utils.MyApplication.*;
import static app.zqb.link.Utils.T.showShort;


public class MainActivity extends AppCompatActivity {

    private int xSize = 9;
    private int ySize = 10;

    // 游戏配置对象
    private GameConf config;
    // 游戏业务逻辑接口
    private GameService gameService;
    // 游戏界面
    private GameView gameView;
    // 记录剩余时间的TextView
    private TextView timeTextView;
    // 开始按钮
    private Button startButton;
    // 失败后弹出的对话框
    private AlertDialog.Builder lostDialog;
    // 游戏胜利后的对话框
    private AlertDialog.Builder successDialog;
    // 定时器
    private Timer timer = new Timer();
    // 记录游戏的剩余时间
    private int gameTime;
    // 记录是否处于游戏状态
    private boolean isPlaying;

    private LinearLayout gameLayout;

    // 记录已经选中的方块
    private Piece selected = null;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg){
            switch (msg.what){
                case 0x123:
                    timeTextView.setText("剩余时间：" + gameTime);
                    gameTime--;
                    // 时间小于0, 游戏失败
                    if(gameTime < 0){
                        stopTimer();
                        // 更改游戏的状态
                        isPlaying = false;
                        lostDialog.show();
                        return;
                    }
                    break;
            }
        }
    };

    // 播放音效的SoundPool
    SoundPool soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
    int dis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onPause()
    {
        // 暂停游戏
        stopTimer();
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        // 如果处于游戏状态中
        if (isPlaying)
        {
            // 以剩余时间重写开始游戏
            startGame(gameTime);
        }
        super.onResume();
    }

    // 初始化游戏的方法
    private void init(){
        gameLayout = (LinearLayout) findViewById(R.id.game_layout);
        gameView = (GameView) findViewById(R.id.game_view);
        timeTextView = (TextView) findViewById(R.id.time_text);
        startButton = (Button) findViewById(R.id.start_button);
        dis = soundPool.load(this, R.raw.dis, 1);

        Bitmap bn = BitmapFactory.decodeResource(this.getResources(), R.mipmap.p_1);
        GameConf.PIECE_WIDTH = bn.getWidth();
        GameConf.PIECE_HEIGHT = bn.getHeight();

       gameLayout.getViewTreeObserver().
               addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                gameLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                config = new GameConf(xSize, ySize, 0, 0, 100000, MainActivity.this);
                Bitmap bm = BitmapFactory.decodeResource(MainActivity.this.getResources(), R.mipmap.p_1);
                config.setBeginImageX((gameLayout.getMeasuredWidth() - bm.getWidth() * xSize) / 2);
                config.setBeginImageY((gameLayout.getMeasuredHeight() - bm.getHeight() * ySize) / 2);
                gameService = new GameServiceImpl(config);
                gameView.setGameService(gameService);
            }
        });

        // 为开始按钮的单击事件绑定事件监听器
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame(GameConf.DEFAULT_TIME);
            }
        });
        // 为游戏区域的触碰事件绑定监听器
        this.gameView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isPlaying) {
                    return false;
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    gameViewTouchDown(event);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    MainActivity.this.gameView.postInvalidate();
                }
                return true;
            }
        });
        lostDialog = createDialog("Lost", "游戏失败！重新开始", R.mipmap.lost).
                setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startGame(GameConf.DEFAULT_TIME);
            }
        });
        successDialog = createDialog("Success", "游戏胜利！重新开始", R.mipmap.success).
                setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startGame(GameConf.DEFAULT_TIME);
            }
        });
    }

    // 触碰游戏区域的处理方法
    private void gameViewTouchDown(MotionEvent event){
        Piece[][] pieces = gameService.getPieces();
        float touchX = event.getX();
        float touchY = event.getY();
        Piece currentPiece = gameService.findPiece(touchX, touchY);
        if(currentPiece.getIndexX() == 999){
            L.d("null null null null");
            return;
        }
        this.gameView.setSelectedPiece(currentPiece);
        if(this.selected == null){
            this.selected = currentPiece;
            this.gameView.postInvalidate();
            return;
        }
        if(this.selected != null){
            LinkInfo linkInfo = this.gameService.link(this.selected, currentPiece);
            if(linkInfo == null){
                this.selected = currentPiece;
                this.gameView.postInvalidate();
            }else {
                handleSuccessLink(linkInfo, this.selected, currentPiece, pieces);
            }
        }

    }

    /**
     * 成功连接后处理
     *
     * @param linkInfo 连接信息
     * @param prePiece 前一个选中方块
     * @param currentPiece 当前选择方块
     * @param pieces 系统中还剩的全部方块
     */
    private void handleSuccessLink(LinkInfo linkInfo, Piece prePiece, Piece currentPiece, Piece[][] pieces){

        this.gameView.setLinkInfo(linkInfo);
        this.gameView.setSelectedPiece(null);
        pieces[prePiece.getIndexX()][prePiece.getIndexY()] = null;
        pieces[currentPiece.getIndexX()][currentPiece.getIndexY()] = null;
        this.gameView.postInvalidate();

        this.selected = null;
        soundPool.play(dis, 1, 1, 0, 0, 1);

        if(!this.gameService.hasPieces()){
            this.successDialog.show();
            stopTimer();
            isPlaying = false;
        }
    }

    // 创建对话框的工具方法
    private AlertDialog.Builder createDialog(String title, String message, int imageResource){
        return new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle(title).setMessage(message).setIcon(imageResource);
    }

    // 以gameTime作为剩余时间开始或恢复游戏
    private void startGame(int gameTime){
        // 如果之前的timer还未取消，取消timer
        if(this.timer != null){
            stopTimer();
        }
        // 重新设置游戏时间
        this.gameTime = gameTime;
        // 如果游戏剩余时间与总游戏时间相等，即为重新开始新游戏
        if(gameTime == GameConf.DEFAULT_TIME){
            // 开始新的游戏
            gameView.startGame();
        }
        isPlaying = true;
        this.timer = new Timer();
        // 启动计时器 ， 每隔1秒发送一次消息
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0x123);
            }
        }, 0, 1000);
        // 将选中方块设为null。
        this.selected = null;
    }

    // 停止定时器
    private void stopTimer(){
        this.timer.cancel();
        this.timer = null;
    }
}
