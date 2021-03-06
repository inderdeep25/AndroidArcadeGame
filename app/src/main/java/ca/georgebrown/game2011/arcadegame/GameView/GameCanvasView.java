package ca.georgebrown.game2011.arcadegame.GameView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ca.georgebrown.game2011.arcadegame.GameModels.Bullet;
import ca.georgebrown.game2011.arcadegame.GameModels.Enemy;
import ca.georgebrown.game2011.arcadegame.GameModels.Player;
import ca.georgebrown.game2011.arcadegame.GameModels.Position;
import ca.georgebrown.game2011.arcadegame.R;
import ca.georgebrown.game2011.arcadegame.GameModels.Sprite;

/**
 * Created by jamie on 08/04/2018.
 */

public class GameCanvasView extends SurfaceView implements Runnable {

    Thread ourThread = null;
    SurfaceHolder ourHolder;
    boolean isGamePlaying;

    long lastFrameTime;
    long lastUpdateTime;
    int fps;

    private Canvas canvas;
    private Sprite background, scoreBgr, timerBgr, heartFull, heartEmpty, bombFull, bombEmpty, bombButton, pauseButton;
    private Player player;


    private int screenWidth;
    private int screenHeight;
    int hudAreaHeight;

    int livesLeft = 3;
    int bombsLeft = 5;
    int bulletShootInterval = 1;

    ArrayList<Sprite> lifeLeftIcons = new ArrayList<>();
    ArrayList<Sprite> bombsLeftIcons = new ArrayList<>();

    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Enemy> enemies = new ArrayList<>();

    int enemySpeedBoost = 0;

    public GameCanvasView(Context context, AttributeSet attrs){
        super(context,attrs);
        setupGameCanvasView();
    }

    public GameCanvasView(Context context){
        super(context);
        setupGameCanvasView();
    }

    private void setupGameCanvasView() {

        ourHolder = getHolder();
        initializeScreenMeasurements();
        initializeHUDElementsVariables();
        setupPlayer();

    }

    private void initializeScreenMeasurements(){

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        hudAreaHeight = screenHeight/4;

    }


    private void initializeHUDElementsVariables() {

        //Background
        Bitmap backgroundBMP = BitmapFactory.decodeResource(getResources(), R.mipmap.background_gameplay);
        Position backgroundPosition = new Position(0,0);
        background = new Sprite(backgroundBMP,backgroundPosition,screenHeight,screenWidth);

        //Layer 1 HUD Elements
        Bitmap scoreBgrBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.hud_score);
        Position scorePosition = new Position(10,10);
        scoreBgr = new Sprite(scoreBgrBMP,scorePosition,hudAreaHeight/3 - 10,screenWidth/4);

        Bitmap timerBgrBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.hud_timer);
        Position timerPosition = new Position(screenWidth/4 + 20,20);
        timerBgr = new Sprite(timerBgrBMP,timerPosition,hudAreaHeight/3 - 20,3*screenWidth/4 - 40);


        //Layer 2 HUD Elements
        Bitmap heartFullBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.hud_fullheart);

        for (int i = 0; i < livesLeft; i++){
            int lifeIconWidthHeight = hudAreaHeight/3;
            Position lifeIconPosition = new Position(scorePosition.getLeftPosition()+(i*lifeIconWidthHeight)+10,hudAreaHeight/3);
            Sprite lifeIcon = new Sprite(heartFullBMP,lifeIconPosition,lifeIconWidthHeight,lifeIconWidthHeight);
            lifeLeftIcons.add(lifeIcon);
        }

        Bitmap bombFullBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.hud_fullbomb);

        for (int i = 0; i < bombsLeft; i++){
            int bombIconWidthHeight = hudAreaHeight/3;
            Position bombIconPosition = new Position((screenWidth/4)+(i*bombIconWidthHeight) + 30,hudAreaHeight/3);
            Sprite bombIcon = new Sprite(bombFullBMP,bombIconPosition,bombIconWidthHeight,bombIconWidthHeight);
            bombsLeftIcons.add(bombIcon);
        }

        //Layer 3 HUD Elements
        Bitmap pauseButtonBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.button_pause);
        Position pauseButtonPosition = new Position(20,2*hudAreaHeight/3);
        pauseButton = new Sprite(pauseButtonBMP,pauseButtonPosition,hudAreaHeight/3,hudAreaHeight/3);

        Bitmap bombButtonBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.button_bomb);
        Position bombButtonPosition = new Position(hudAreaHeight/3+30,2*hudAreaHeight/3);
        bombButton = new Sprite(bombButtonBMP,bombButtonPosition,hudAreaHeight/3,hudAreaHeight/3);


    }

    private void setupPlayer() {

        Bitmap playerMinionImage = BitmapFactory.decodeResource(getResources(),R.mipmap.player_helper);
        Bitmap playerImage = BitmapFactory.decodeResource(getResources(),R.mipmap.player);
        Position playerPosition = new Position(150,350);
        player = new Player(playerImage,playerPosition,playerMinionImage);

    }

    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        this.draw();

    }

    private boolean isEnemyOutsideTheScreen(Enemy enemy){

        boolean result = false;

        if(enemy.getEnemyRect().left < 0){
            result = true;
        }

        return result;

    }

    private boolean isBulletOutsideTheScreen(Bullet bullet){

        boolean result = false;

        if(bullet.getIconRect().left > screenWidth){
            result = true;
        }

        return result;

    }

    private void generateEnemyRandomly(){

        Bitmap enemyType1 = BitmapFactory.decodeResource(getResources(),R.mipmap.enemy_minion_17);
        Bitmap enemyType2 = BitmapFactory.decodeResource(getResources(),R.mipmap.enemy_minion_18);
        Random rand = new Random();
        int upperBound = (screenHeight - player.playerHeight);
        int lowerBound = hudAreaHeight + player.topMinionHeight + 20;
        int randomHeight = lowerBound + rand.nextInt(upperBound);

        //Prevent enemy to spawn out of screen bounds
        if(randomHeight > screenHeight - player.playerHeight){
            randomHeight = screenHeight - player.playerHeight;
        }

        Position enemyPosition = new Position(screenWidth + 100,randomHeight);

        Enemy enemy;
        int randomNumber = rand.nextInt();
        if (randomNumber % 2 == 0){
            enemy = new Enemy(enemyType1,enemyPosition,10 + enemySpeedBoost);
        }else{
            enemy = new Enemy(enemyType2,enemyPosition,15 + enemySpeedBoost);
        }

        enemies.add(enemy);
    }

    private Bullet[] generateBullets() {

        Bullet[] result = new Bullet[4];

        Bitmap bulletBMP = BitmapFactory.decodeResource(getResources(),R.mipmap.player_bullet_03);

        Rect minionRect = player.getTopMinionRect();

        Position bulletPosition = new Position(minionRect.left,minionRect.top);
        result[0] = new Bullet(bulletBMP,bulletPosition,25,40);

        minionRect = player.getBottomMinionRect();

        bulletPosition = new Position(minionRect.left,minionRect.top);
        result[1] = new Bullet(bulletBMP,bulletPosition,25,40);

        minionRect = player.getTopMinionRect();

        bulletPosition = new Position(minionRect.left+player.playerWidth,minionRect.top + 50);
        result[2] = new Bullet(bulletBMP,bulletPosition,25,40);

        minionRect = player.getBottomMinionRect();

        bulletPosition = new Position(minionRect.left+player.playerWidth,minionRect.top - 50);
        result[3] = new Bullet(bulletBMP,bulletPosition,25,40);

//        result[2].bulletSpeed = 30;
//        result[3].bulletSpeed = 30;

        return result;
    }

    private boolean isBulletAlreadyOnScreen(String bulletId){

        boolean result = false;

        for(Bullet bullet : bullets){
            if (bullet.bulletId == bulletId){
                result = true;
                break;
            }
        }

        return result;

    }
    int bulletGap = 0;
    int enemyGap = 0;
    private void update(){

        Date date = new Date();

        bulletGap++;

        long timeDeltaMiliSeconds = date.getTime() - lastUpdateTime;
        int timeDelta = (int) TimeUnit.MILLISECONDS.toSeconds(timeDeltaMiliSeconds);

        //Increase speed of enemies every 15 seconds
        if(timeDelta % 15 == 0){
            enemySpeedBoost += 1;
        }

        //Generate bullets every few milliseconds
        if (timeDelta % bulletShootInterval == 0 && bulletGap % 10 == 0){


            if (!isBulletAlreadyOnScreen(Integer.toString(timeDelta))){
                Bullet[] bulletsArray = generateBullets();
                for(Bullet bullet : bulletsArray){
                    bullet.bulletId = Integer.toString(timeDelta);
                    bullets.add(bullet);
                }

            }
            else if (!isBulletAlreadyOnScreen(Integer.toString(timeDelta)+Integer.toString(bulletGap))){
                Bullet[] bulletsArray = generateBullets();
                for(Bullet bullet : bulletsArray){
                    bullet.bulletId = Integer.toString(timeDelta)+Integer.toString(bulletGap);
                    bullets.add(bullet);
                }

            }

        }

        enemyGap += 3;
        if (timeDelta % 2 == 0 && enemyGap % 4 == 0){
            generateEnemyRandomly();
        }

        //Move Bullets
        for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext(); ) {
            Bullet bullet = iterator.next();
            bullet.moveForward();
            if (isBulletOutsideTheScreen(bullet)){
                iterator.remove();
            }
        }

        //Move Enemies

        for (Iterator<Enemy> iterator = enemies.iterator(); iterator.hasNext(); ) {
            Enemy enemy = iterator.next();
            enemy.moveEnemy();
            if (isEnemyOutsideTheScreen(enemy)){
                iterator.remove();
            }
        }

    }

    private void draw(){
        if (ourHolder.getSurface().isValid()) {
            canvas = ourHolder.lockCanvas();
            canvas.drawColor(Color.BLACK);
            background.drawIconOnCanvas(canvas);
            scoreBgr.drawIconOnCanvas(canvas);
            timerBgr.drawIconOnCanvas(canvas);

            for (Sprite icon : lifeLeftIcons) {
                icon.drawIconOnCanvas(canvas);
            }

            for (Sprite icon : bombsLeftIcons) {
                icon.drawIconOnCanvas(canvas);
            }

            pauseButton.drawIconOnCanvas(canvas);
            bombButton.drawIconOnCanvas(canvas);

            player.drawPlayerOnCanvas(canvas);

            for(Bullet bullet : bullets){
                bullet.drawIconOnCanvas(canvas);
            }

            for(Enemy enemy : enemies){
                enemy.drawEnemyOnCanvas(canvas);
            }

            ourHolder.unlockCanvasAndPost(canvas);

        }
    }

    public void controlFPS(){
        long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
        long timeToSleep = 50 - timeThisFrame;

        if(timeThisFrame > 0) {
            fps = (int)(1000/timeThisFrame);
        }

        if (timeToSleep > 0) {
            try {
                ourThread.sleep(timeToSleep);
            } catch (InterruptedException e) {
            }
        }

        lastFrameTime = System.currentTimeMillis();
    }

    private void checkPlayerCollisionWithEnemies(){

        for (Iterator<Enemy> enemyIterator = enemies.iterator(); enemyIterator.hasNext(); ) {
            Enemy enemy = enemyIterator.next();

            int left = player.getPlayerRect().left;
            int top = player.getPlayerRect().top;
                int right = player.getPlayerRect().right;
                int bottom = player.getPlayerRect().bottom;

            if ((left > enemy.getEnemyRect().left
                    && left < enemy.getEnemyRect().right
                    && top > enemy.getEnemyRect().top
                    && top < enemy.getEnemyRect().bottom)
                    || (right > enemy.getEnemyRect().left
                    && right < enemy.getEnemyRect().right
                    && bottom > enemy.getEnemyRect().top
                    && bottom < enemy.getEnemyRect().bottom)){

                takeLife();
                break;
            }
        }

    }

    private void checkBulletCollisionWithEnemies(){

        for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext(); ) {

            Bullet bullet = iterator.next();

            boolean shouldRemoveBullet = false;
            for (Iterator<Enemy> enemyIterator = enemies.iterator(); enemyIterator.hasNext(); ) {
                Enemy enemy = enemyIterator.next();

                int left = bullet.getIconRect().left;
                int top = bullet.getIconRect().top;
//                int right = bullet.getIconRect().right;
//                int bottom = bullet.getIconRect().bottom;

                if (left > enemy.getEnemyRect().left
                        && left < enemy.getEnemyRect().right
                        && top > enemy.getEnemyRect().top
                        && top < enemy.getEnemyRect().bottom){

                    enemyIterator.remove();
                    shouldRemoveBullet = true;
                    break;
                }
            }

            if (shouldRemoveBullet){
                iterator.remove();
            }

        }



    }

    @Override
    public void run() {
        while (isGamePlaying){
            checkPlayerCollisionWithEnemies();
            checkBulletCollisionWithEnemies();
            update();
            draw();
            controlFPS();
        }
    }

    public void pause() {
        isGamePlaying = false;
        try {
            ourThread.join();
        } catch (InterruptedException e) {
        }
    }

    public void resume() {
        isGamePlaying = true;
        ourThread = new Thread(this);
        ourThread.start();
    }

    public void takeLife(){
        if(livesLeft > 0){
            livesLeft--;
            int lastIndex = lifeLeftIcons.size() - 1;
            lifeLeftIcons.remove(lastIndex);
            resetGame();
        }
        else{
            //TODO: GAME OVER HERE
            pause();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction()==MotionEvent.ACTION_DOWN) {

            if (x > pauseButton.getIconRect().left
                    && x < pauseButton.getIconRect().right
                    && y > pauseButton.getIconRect().top
                    && y < pauseButton.getIconRect().bottom){

                if(isGamePlaying) {
                    pause();
                }else{
                    resume();
                }

            }

            if (x > bombButton.getIconRect().left
                    && x < bombButton.getIconRect().right
                    && y > bombButton.getIconRect().top
                    && y < bombButton.getIconRect().bottom
                    && bombsLeft > 0){

                bombsLeft--;
                enemies = new ArrayList<>();
                int lastIndex = bombsLeftIcons.size()-1;
                bombsLeftIcons.remove(lastIndex);

                if (bombsLeft <= 0){
                    Bitmap noBombsLeftIcon = BitmapFactory.decodeResource(getResources(),R.mipmap.button_bomb_empty);
                    bombButton.setIconImage(noBombsLeftIcon);
                }

            }

            return true;
        }

        if (event.getAction()==MotionEvent.ACTION_MOVE){

            this.updatePlayerPositionTo(x,y);
            return true;

        }

        return false;
    }

    public void updatePlayerPositionTo(int x, int y){

        if(y > screenHeight - player.playerHeight){
            y = screenHeight - player.playerHeight;
        }

        if (y < hudAreaHeight + player.topMinionHeight + 20){
            y = hudAreaHeight + player.topMinionHeight + 20;
        }

        if (x > screenWidth - player.playerWidth){
            x = screenWidth - player.playerWidth;
        }

        player.setPlayerPosition(new Position(x,y));
    }

    public void resetGame(){
        bullets = new ArrayList<>();
        enemies = new ArrayList<>();
        lifeLeftIcons = new ArrayList<>();
        bombsLeftIcons = new ArrayList<>();
        setupGameCanvasView();
    }


}
