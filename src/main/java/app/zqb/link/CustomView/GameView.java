package app.zqb.link.CustomView;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import app.zqb.link.Game.GameConf;
import app.zqb.link.Game.GameService;
import app.zqb.link.Game.LinkInfo;
import app.zqb.link.R;
import app.zqb.link.Utils.ImageUtil;
import app.zqb.link.Utils.L;

/**
 * TODO: document your custom view class.
 */
public class GameView extends View {

    // 游戏逻辑的实现类
    private GameService gameService;

    // 保存当前已经被选中的方块
    private Piece selectedPiece;

    // 连接信息对象
    private LinkInfo linkInfo;

    private Paint paint;

    // 选中标识的图片对象
    private Bitmap selectImage;

    public GameView(Context context, AttributeSet attrs){
        super(context, attrs);
        this.paint = new Paint();
        this.paint.setShader(new BitmapShader(BitmapFactory.decodeResource(context.getResources(),
                R.mipmap.heart), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        this.paint.setStrokeWidth(9);
        this.selectImage = ImageUtil.getSelectImage(context);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(this.gameService == null){
            return;
        }
        Piece[][] pieces = gameService.getPieces();
        if(pieces != null){
            // 遍历pieces二维数组
            for(int i = 0; i < pieces.length; i++){
                for (int j = 0; j < pieces[i].length; j++){
                    // 如果二维数组中该元素不为空（即有方块），将这个方块的图片画出来
                    if(pieces[i][j] != null){
                        // 得到这个Piece对象
                        Piece piece = pieces[i][j];
                        // 根据方块左上角X、Y座标绘制方块
                        canvas.drawBitmap(piece.getImage().getImage(),
                                piece.getBeginX(), piece.getBeginY(), null);
                    }
                }
            }
        }
        // 如果当前对象中有linkInfo对象, 即连接信息
        if(this.linkInfo != null){
            drawLine(this.linkInfo, canvas);
            this.linkInfo = null;
        }
        if (this.selectedPiece != null){
            canvas.drawBitmap(this.selectImage,
                    this.selectedPiece.getBeginX(), this.selectedPiece.getBeginY(), null);
        }
    }

    // 根据LinkInfo绘制连接线的方法。
    private void drawLine(LinkInfo linkInfo, Canvas canvas){
        // 获取LinkInfo中封装的所有连接点
        List<Point> points = linkInfo.getLinkPoints();
        // 依次遍历linkInfo中的每个连接点
        for (int i = 0; i < points.size() - 1; i++){
            // 获取当前连接点与下一个连接点
            Point currentPoint = points.get(i);
            Point nextPoint = points.get(i + 1);
            L.d(points.get(i).x + "," + points.get(i).y);
            L.d(points.get(i + 1).x + "," + points.get(i + 1).y);
            // 绘制连线
            canvas.drawLine(currentPoint.x, currentPoint.y, nextPoint.x, nextPoint.y, this.paint);
        }
    }

    public void setLinkInfo(LinkInfo linkInfo){
        this.linkInfo = linkInfo;
    }

    public void setGameService(GameService gameService){
        this.gameService = gameService;
    }

    public void setSelectedPiece(Piece piece){
        this.selectedPiece = piece;
    }

    public void startGame(){
        this.gameService.start();
        this.postInvalidate();
    }

}
