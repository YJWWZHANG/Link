package app.zqb.link.Game;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import app.zqb.link.CustomView.Piece;
import app.zqb.link.Utils.L;

/**
 * Created by admin on 2016/2/28.
 */
public class GameServiceImpl implements GameService
{
    // 定义一个Piece[][]数组，只提供getter方法
    private Piece[][] pieces;

    // 游戏配置对象
    private GameConf config;

    public GameServiceImpl(GameConf config){
        this.config = config;
    }

    @Override
    public void start() {
        AbstractBoard board = null;
        Random random = new Random();
        int index = random.nextInt(4);
        switch (index){
            case 0:
                board = new VerticalBoard();
                break;
            case 1:
                board = new HorizontalBoard();
                break;
            default:
                board = new FullBoard();
                break;
        }
        this.pieces = board.create(config);
    }

    // 直接返回本对象的Piece[][]数组
    @Override
    public Piece[][] getPieces()
    {
        return this.pieces;
    }

    // 根据触碰点的位置查找相应的方块
    @Override
    public Piece findPiece(float touchX, float touchY) {
        int relativeX = (int) touchX - config.getBeginImageX();
        int relativeY = (int) touchY - config.getBeginImageY();
        if(relativeX < 0 || relativeY < 0){
            return new Piece(999, 999);
        }
        int indexX = getIndex(relativeX, GameConf.PIECE_WIDTH);
        int indexY = getIndex(relativeY, GameConf.PIECE_HEIGHT);
        if(indexX < 0 || indexY < 0){
            return new Piece(999, 999);
        }
        if(indexX >= this.config.getXSize() || indexY >= this.config.getYSize()){
            return new Piece(999, 999);
        }
        if(this.pieces[indexX][indexY] == null){
            return new Piece(999, 999);
        }
        return this.pieces[indexX][indexY];
    }

    @Override
    public LinkInfo link(Piece p1, Piece p2) {
        if (p1.equals(p2)){
            return null;
        }
        if(!p1.isSameImage(p2)){
            return null;
        }
        if(p2.getIndexX() < p1.getIndexX()){
            L.d("p2在p1左边，参数互换");
            return link(p2, p1);
        }
        Point p1Point = p1.getCenter();
        Point p2Point = p2.getCenter();

        if(p1.getIndexY() == p2.getIndexY()){
            if(!isXBlock(p1Point, p2Point, GameConf.PIECE_WIDTH)){
                return new LinkInfo(p1Point, p2Point);
            }
        }
        if(p1.getIndexX() == p2.getIndexX()){
            if(!isYBlock(p1Point, p2Point, GameConf.PIECE_HEIGHT)){
                return new LinkInfo(p1Point, p2Point);
            }
        }

        Point cornerPoint = getCornerPoint(p1Point, p2Point, GameConf.PIECE_WIDTH, GameConf.PIECE_HEIGHT);
        if(cornerPoint != null){
            return new LinkInfo(p1Point, cornerPoint, p2Point);
        }

        Map<Point, Point> turns = getLinkPoints(p1Point, p2Point,
                GameConf.PIECE_WIDTH, GameConf.PIECE_HEIGHT);
        if (turns.size() != 0){
            return getShortcut(p1Point, p2Point, turns, getDistance(p1Point, p2Point));
        }
        return null;
    }

    @Override
    public boolean hasPieces() {
        // 遍历Piece[][]数组的每个元素
        for (int i = 0; i < pieces.length; i++)
        {
            for (int j = 0; j < pieces[i].length; j++)
            {
                // 只要任意一个数组元素不为null，也就是还剩有非空的Piece对象
                if (pieces[i][j] != null)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private LinkInfo getShortcut(Point p1, Point p2, Map<Point, Point> turns, int shortDistance){
        List<LinkInfo> infos = new ArrayList<LinkInfo>();
        for (Point point1 : turns.keySet()){
            Point point2 = turns.get(point1);
            infos.add(new LinkInfo(p1, point1, point2, p2));
        }
        return getShortcut(infos, shortDistance);
    }

    private LinkInfo getShortcut(List<LinkInfo> infos, int shortDistance){
        int temp1 = 0;
        LinkInfo result = null;
        for (int i = 0; i < infos.size(); i++){
            LinkInfo info = infos.get(i);
            int distance = countAll(info.getLinkPoints());
            if (i == 0){
                temp1 = distance - shortDistance;
                result = info;
            }
            if(distance - shortDistance < temp1){
                temp1 = distance - shortDistance;
                result = info;
            }
        }

        return result;
    }

    private int getDistance(Point p1, Point p2){
        int xDistance = Math.abs(p1.x - p2.x);
        int yDistance = Math.abs(p1.y - p2.y);
        return  xDistance + yDistance;
    }

    private int countAll(List<Point> points){
        int result = 0;
        for (int i = 0; i < points.size() - 1; i++){
            Point point1 = points.get(i);
            Point point2 = points.get(i + 1);
            result += getDistance(point1, point2);
        }
        return result;
    }

    private Map<Point, Point> getLinkPoints(Point point1, Point point2, int pieceWidth, int pieceHeight){
        Map<Point, Point> result = new HashMap<Point, Point>();

        List<Point> p1UpChanel = getUpChanel(point1, point2.y, pieceHeight);
        List<Point> p1RightChanel = getRightChanel(point1, point2.x, pieceWidth);
        List<Point> p1DownChanel = getDownChanel(point1, point2.y, pieceHeight);

        List<Point> p2DownChanel = getDownChanel(point2, point1.y, pieceHeight);
        List<Point> p2LeftChanel = getLeftChanel(point2, point1.x, pieceWidth);
        List<Point> p2UpChanel = getUpChanel(point2, point1.y, pieceHeight);

        int heightMax = (this.config.getYSize() + 1) * pieceHeight + this.config.getBeginImageY();
        int widthMax = (this.config.getXSize() + 1) * pieceWidth + this.config.getBeginImageX();

        if(isLeftDown(point1, point2) || isLeftUp(point1, point2)){
            return getLinkPoints(point2, point1, pieceWidth, pieceHeight);
        }

        if (point1.y == point2.y){
            p1UpChanel = getUpChanel(point1, 0, pieceHeight);
            p2UpChanel = getUpChanel(point2, 0, pieceHeight);
            Map<Point, Point> upLinkPoints = getXLinkPoints(p1UpChanel, p2UpChanel, pieceWidth);


            p1DownChanel = getDownChanel(point1, heightMax, pieceHeight);
            p2DownChanel = getDownChanel(point2, heightMax, pieceHeight);
            Map<Point, Point> downLinkPoints = getXLinkPoints(p1DownChanel, p2DownChanel, pieceWidth);

            result.putAll(upLinkPoints);
            result.putAll(downLinkPoints);
        }

        if (point1.x == point2.x){
            List<Point> p1LeftChanel = getLeftChanel(point1, 0, pieceWidth);
            p2LeftChanel = getLeftChanel(point2, 0, pieceWidth);
            Map<Point, Point> leftLinkPoints = getYLinkPoints(p1LeftChanel, p2LeftChanel, pieceWidth);

            p1RightChanel = getRightChanel(point1, widthMax, pieceWidth);
            List<Point> p2RightChanel = getRightChanel(point2, widthMax, pieceWidth);
            Map<Point, Point> rightLinkPoints = getYLinkPoints(p1RightChanel, p2RightChanel, pieceHeight);

            result.putAll(leftLinkPoints);
            result.putAll(rightLinkPoints);
        }

        if(isRightUp(point1, point2)){
            L.d("右上");
            Map<Point, Point> upDownLinkPoints = getXLinkPoints(p1UpChanel, p2DownChanel, pieceWidth);
            Map<Point, Point> leftRightLinkPoints = getYLinkPoints(p1RightChanel, p2LeftChanel, pieceHeight);

            p1UpChanel = getUpChanel(point1, 0, pieceHeight);
            p2UpChanel = getUpChanel(point2, 0, pieceHeight);
            Map<Point, Point> upUpLinkPoints = getXLinkPoints(p1UpChanel,
                    p2UpChanel, pieceWidth);

            p1DownChanel = getDownChanel(point1, heightMax, pieceHeight);
            p2DownChanel = getDownChanel(point2, heightMax, pieceHeight);
            Map<Point, Point> downDownLinkPoints = getXLinkPoints(p1DownChanel,
                    p2DownChanel, pieceWidth);

            p1RightChanel = getRightChanel(point1, widthMax, pieceWidth);
            List<Point> p2RightChanel = getRightChanel(point2, widthMax,
                    pieceWidth);
            Map<Point, Point> rightRightLinkPoints = getYLinkPoints(
                    p1RightChanel, p2RightChanel, pieceHeight);

            List<Point> p1LeftChanel = getLeftChanel(point1, 0, pieceWidth);
            p2LeftChanel = getLeftChanel(point2, 0, pieceWidth);
            Map<Point, Point> leftLeftLinkPoints = getYLinkPoints(p1LeftChanel,
                    p2LeftChanel, pieceHeight);

            result.putAll(upDownLinkPoints);
            result.putAll(leftRightLinkPoints);
            result.putAll(upUpLinkPoints);
            result.putAll(downDownLinkPoints);
            result.putAll(rightRightLinkPoints);
            result.putAll(leftLeftLinkPoints);
        }

        if(isRightDown(point1, point2)){
            L.d("右下");
            Map<Point, Point> upDownLinkPoints = getXLinkPoints(p1DownChanel, p2UpChanel, pieceWidth);
            Map<Point, Point> leftRightLinkPoints = getYLinkPoints(p1RightChanel, p2LeftChanel, pieceHeight);

            p1UpChanel = getUpChanel(point1, 0, pieceHeight);
            p2UpChanel = getUpChanel(point2, 0, pieceHeight);
            Map<Point, Point> upUpLinkPoints = getXLinkPoints(p1UpChanel,
                    p2UpChanel, pieceWidth);

            p1DownChanel = getDownChanel(point1, heightMax, pieceHeight);
            p2DownChanel = getDownChanel(point2, heightMax, pieceHeight);
            Map<Point, Point> downDownLinkPoints = getXLinkPoints(p1DownChanel,
                    p2DownChanel, pieceWidth);

            List<Point> p1LeftChanel = getLeftChanel(point1, 0, pieceWidth);
            p2LeftChanel = getLeftChanel(point2, 0, pieceWidth);
            Map<Point, Point> leftLeftLinkPoints = getYLinkPoints(p1LeftChanel,
                    p2LeftChanel, pieceHeight);

            p1RightChanel = getRightChanel(point1, widthMax, pieceWidth);
            List<Point> p2RightChanel = getRightChanel(point2, widthMax,
                    pieceWidth);
            Map<Point, Point> rightRightLinkPoints = getYLinkPoints(
                    p1RightChanel, p2RightChanel, pieceHeight);

            result.putAll(upDownLinkPoints);
            result.putAll(leftRightLinkPoints);
            result.putAll(upUpLinkPoints);
            result.putAll(downDownLinkPoints);
            result.putAll(leftLeftLinkPoints);
            result.putAll(rightRightLinkPoints);
        }

        return result;
    }

    /**
     * 遍历两个集合, 先判断第一个集合的元素的y座标与另一个集合中的元素y座标相同(横向),
     * 如果相同, 即在同一行, 再判断是否有障碍, 没有 则加到结果的map中去
     *
     * @param p1Chanel
     * @param p2Chanel
     * @param pieceWidth
     * @return 存放可以横向直线连接的连接点的键值对
     */
    private Map<Point, Point> getXLinkPoints(List<Point> p1Chanel, List<Point> p2Chanel, int pieceWidth)
    {
        Map<Point, Point> result = new HashMap<Point, Point>();
        for (int i = 0; i < p1Chanel.size(); i++)
        {
            // 从第一通道中取一个点
            Point temp1 = p1Chanel.get(i);
            // 再遍历第二个通道, 看下第二通道中是否有点可以与temp1横向相连
            for (int j = 0; j < p2Chanel.size(); j++)
            {
                Point temp2 = p2Chanel.get(j);
                // 如果y座标相同(在同一行), 再判断它们之间是否有直接障碍
                if (temp1.y == temp2.y)
                {
                    if (!isXBlock(temp1, temp2, pieceWidth))
                    {
                        // 没有障碍则直接加到结果的map中
                        result.put(temp1, temp2);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 遍历两个集合, 先判断第一个集合的元素的x座标与另一个集合中的元素x座标相同(纵向),
     * 如果相同, 即在同一列, 再判断是否有障碍, 没有则加到结果的Map中去
     *
     * @param p1Chanel
     * @param p2Chanel
     * @param pieceHeight
     * @return
     */
    private Map<Point, Point> getYLinkPoints(List<Point> p1Chanel,
                                             List<Point> p2Chanel, int pieceHeight)
    {
        Map<Point, Point> result = new HashMap<Point, Point>();
        for (int i = 0; i < p1Chanel.size(); i++)
        {
            Point temp1 = p1Chanel.get(i);
            for (int j = 0; j < p2Chanel.size(); j++)
            {
                Point temp2 = p2Chanel.get(j);
                // 如果x座标相同(在同一列)
                if (temp1.x == temp2.x)
                {
                    // 没有障碍, 放到map中去
                    if (!isYBlock(temp1, temp2, pieceHeight))
                    {
                        result.put(temp1, temp2);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 获取两个不在同一行或者同一列的座标点的直角连接点, 即只有一个转折点
     *
     * @param point1 第一个点
     * @param point2 第二个点
     * @return 两个不在同一行或者同一列的座标点的直角连接点
     */
    private Point getCornerPoint(Point point1, Point point2, int pieceWidth, int pieceHeight){

        if(isLeftUp(point1, point2) || isLeftDown(point1, point2)){
            return getCornerPoint(point2, point1, pieceWidth, pieceHeight);
        }

        List<Point> point1RightChanel = getRightChanel(point1, point2.x, pieceWidth);
        List<Point> point1UpChanel = getUpChanel(point1, point2.y, pieceHeight);
        List<Point> point1DownChanel = getDownChanel(point1, point2.y, pieceHeight);

        List<Point> point2DownChanel = getDownChanel(point2, point1.y, pieceHeight);
        List<Point> point2LeftChanel = getLeftChanel(point2, point1.x, pieceWidth);
        List<Point> point2UpChanel = getUpChanel(point2, point1.y, pieceHeight);

        if(isRightUp(point1, point2)){
            Point linkPoint1 = getWrapPoint(point1RightChanel, point2DownChanel);
            Point linkPoint2 = getWrapPoint(point1UpChanel, point2LeftChanel);
            return (linkPoint1 == null)? linkPoint2 : linkPoint1;
        }

        if(isRightDown(point1, point2)){
            Point linkPoint1 = getWrapPoint(point1DownChanel, point2LeftChanel);
            Point linkPoint2 = getWrapPoint(point1RightChanel, point2UpChanel);
            return (linkPoint1 == null) ? linkPoint2 : linkPoint1;
        }

        return null;
    }

    /**
     * 遍历两个通道, 获取它们的交点
     *
     * @param p1Chanel 第一个点的通道
     * @param p2Chanel 第二个点的通道
     * @return 两个通道有交点，返回交点，否则返回null
     */
    private Point getWrapPoint(List<Point> p1Chanel, List<Point> p2Chanel){
        for (int i = 0; i < p1Chanel.size(); i++){
            Point temp1 = p1Chanel.get(i);
            for (int j = 0; j < p2Chanel.size(); j++){
                Point temp2 = p2Chanel.get(j);
                if (temp1.equals(temp2)){
                    return temp1;
                }
            }
        }
        return null;
    }

    /**
     * 给一个Point对象,返回它的左边通道
     *
     * @param p
     * @param pieceWidth piece图片的宽
     * @param min 向左遍历时最小的界限
     * @return 给定Point左边的通道
     */
    private List<Point> getLeftChanel(Point p, int min, int pieceWidth)
    {
        List<Point> result = new ArrayList<Point>();
        // 获取向左通道, 由一个点向左遍历, 步长为Piece图片的宽
        for (int i = p.x - pieceWidth; i >= min
                ; i = i - pieceWidth)
        {
            // 遇到障碍, 表示通道已经到尽头, 直接返回
            if (hasPiece(i, p.y))
            {
                return result;
            }
            result.add(new Point(i, p.y));
        }
        return result;
    }

    /**
     * 给一个Point对象, 返回它的右边通道
     *
     * @param p
     * @param pieceWidth
     * @param max 向右时的最右界限
     * @return 给定Point右边的通道
     */
    private List<Point> getRightChanel(Point p, int max, int pieceWidth){
        List<Point> result = new ArrayList<Point>();
        for(int i = p.x + pieceWidth; i <= max; i = i + pieceWidth){
            if(hasPiece(i, p.y)){
                return result;
            }else {
                result.add(new Point(i, p.y));
            }
        }
        return result;
    }

    /**
     * 给一个Point对象, 返回它的上面通道
     *
     * @param p
     * @param min 向上遍历时最小的界限
     * @param pieceHeight
     * @return 给定Point上面的通道
     */
    private List<Point> getUpChanel(Point p, int min, int pieceHeight)
    {
        List<Point> result = new ArrayList<Point>();
        // 获取向上通道, 由一个点向右遍历, 步长为Piece图片的高
        for (int i = p.y - pieceHeight; i >= min
                ; i = i - pieceHeight)
        {
            // 遇到障碍, 表示通道已经到尽头, 直接返回
            if (hasPiece(p.x, i))
            {
                // 如果遇到障碍, 直接返回
                return result;
            }
            result.add(new Point(p.x, i));
        }
        return result;
    }

    /**
     * 给一个Point对象, 返回它的下面通道
     *
     * @param p
     * @param max 向上遍历时的最大界限
     * @return 给定Point下面的通道
     */
    private List<Point> getDownChanel(Point p, int max, int pieceHeight)
    {
        List<Point> result = new ArrayList<Point>();
        // 获取向下通道, 由一个点向右遍历, 步长为Piece图片的高
        for (int i = p.y + pieceHeight; i <= max
                ; i = i + pieceHeight)
        {
            // 遇到障碍, 表示通道已经到尽头, 直接返回
            if (hasPiece(p.x, i))
            {
                // 如果遇到障碍, 直接返回
                return result;
            }
            result.add(new Point(p.x, i));
        }
        return result;
    }

   /**
     * 判断point2是否在point1的左上角
     *
     * @param point1
     * @param point2
     * @return p2位于p1的左上角时返回true，否则返回false
     */
    private boolean isLeftUp(Point point1, Point point2){
        return point2.x < point1.x && point2.y < point1.y;
    }

    /**
     * 判断point2是否在point1的左下角
     *
     * @param point1
     * @param point2
     * @return p2位于p1的左下角时返回true，否则返回false
     */
    private boolean isLeftDown(Point point1, Point point2){
        return point2.x < point1.x && point2.y > point1.y;
    }

    /**
     * 判断point2是否在point1的右上角
     *
     * @param point1
     * @param point2
     * @return p2位于p1的右上角时返回true，否则返回false
     */
    private boolean isRightUp(Point point1, Point point2){
        return point2.x > point1.x && point2.y < point1.y;
    }

    /**
     * 判断point2是否在point1的右下角
     *
     * @param point1
     * @param point2
     * @return p2位于p1的右下角时返回true，否则返回false
     */
    private boolean isRightDown(Point point1, Point point2){
        return point2.x > point1.x && point2.y > point1.y;
    }

    // 工具方法, 根据relative座标计算相对于Piece[][]数组的第一维
    // 或第二维的索引值 ，size为每张图片边的长或者宽
    private int getIndex(int relative, int size){
        int index = -1;
        if(relative % size == 0){
            index = relative / size - 1;
        }else {
            index = relative / size;
        }
        return index;
    }

    /**
     * 判断两个y座标相同的点对象之间是否有障碍, 以p1为中心向右遍历
     *
     * @param p1
     * @param p2
     * @param pieceWidth
     * @return 两个Piece之间有障碍返回true，否则返回false
     */
    private boolean isXBlock(Point p1, Point p2, int pieceWidth){
        if(p2.x < p1.x){
            // 如果p2在p1左边, 调换参数位置调用本方法
            return isXBlock(p2, p1, pieceWidth);
        }
        for(int i = p1.x + pieceWidth; i < p2.x; i = i + pieceWidth){
            // 有障碍
            if(hasPiece(i, p1.y)){
                return true;
            }
        }
        return false;
    }

    /**
     * 判断两个x座标相同的点对象之间是否有障碍, 以p1为中心向下遍历
     *
     * @param p1
     * @param p2
     * @param pieceHeight
     * @return 两个Piece之间有障碍返回true，否则返回false
     */
    private boolean isYBlock(Point p1, Point p2, int pieceHeight)
    {
        if (p2.y < p1.y)
        {
            // 如果p2在p1的上面, 调换参数位置重新调用本方法
            return isYBlock(p2, p1, pieceHeight);
        }
        for (int i = p1.y + pieceHeight; i < p2.y; i = i + pieceHeight)
        {
            if (hasPiece(p1.x, i))
            {
                // 有障碍
                return true;
            }
        }
        return false;
    }

    /**
     * 判断GamePanel中的x, y座标中是否有Piece对象
     *
     * @param x
     * @param y
     * @return true 表示有该座标有piece对象 false 表示没有
     */
    private boolean hasPiece(int x, int y)
    {
        if (findPiece(x, y).getIndexX() == 999) {
            return false;
        }else {
            return true;
        }
    }
}
