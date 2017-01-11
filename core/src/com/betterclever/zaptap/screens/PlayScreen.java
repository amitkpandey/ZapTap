package com.betterclever.zaptap.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.DelayedRemovalArray;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.betterclever.zaptap.Constants;
import com.betterclever.zaptap.Encrypt;
import com.betterclever.zaptap.ZapTapGame;
import com.betterclever.zaptap.objects.CrossMeRing;
import com.betterclever.zaptap.objects.ExplosionTriangle;
import com.betterclever.zaptap.objects.NormalRing;
import com.betterclever.zaptap.objects.PlayBall;
import com.betterclever.zaptap.objects.Ring;
import com.betterclever.zaptap.objects.Score;

/**
 * Created by betterclever on 22/12/16.
 */

public class PlayScreen extends InputAdapter implements Screen {

    private ExtendViewport extendViewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;

    private int sw = 1;
    private float time;

    private float timer;

    int radius;

    private DelayedRemovalArray<Ring> rings;
    private DelayedRemovalArray<ExplosionTriangle> explosionTriangles;

    private PlayBall playBall;

    private Ring lastAttachedRing;
    private ZapTapGame game;
    private Score score;

    private boolean stopped = false;
    private boolean paused = false;
    private boolean gameOver = false;

    private Color bannerColor;

    private BitmapFont font;
    private BitmapFont restartButtonFont;
    private BitmapFont goToHomeFont;

    private Texture pauseButton;
    private Texture zapperImage;
    private Texture sadImage;

    private Rectangle bounds;
    private Rectangle pauseButtonBounds;
    private Rectangle goToHomeBounds;
    private Rectangle continueZapperButtonBounds;

    private int chancesLeft;
    private int playMode;
    private Preferences preferences;

    public PlayScreen(int mode, ZapTapGame zapTapGame) {
        game = zapTapGame;
        playMode = mode;
    }

    @Override
    public void show() {

        shapeRenderer = new ShapeRenderer();
        spriteBatch= new SpriteBatch();
        shapeRenderer.setAutoShapeType(true);
        extendViewport = new ExtendViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT);

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCatchBackKey(true);
        rings = new DelayedRemovalArray<Ring>();
        explosionTriangles = new DelayedRemovalArray<ExplosionTriangle>();

        bounds = new Rectangle();
        time = 0;
        timer = 0;

        preferences = Gdx.app.getPreferences(Constants.PREF_KEY);

        bannerColor = new Color(0,0,0,0.8f);
        score = new Score(spriteBatch);
        playBall = null;

        pauseButton = new Texture("pause.png");
        zapperImage = new Texture("zapper.png");
        sadImage = new Texture("sad.png");

        pauseButtonBounds = new Rectangle(Constants.WORLD_WIDTH-100,Constants.WORLD_HEIGHT-100,100,100);
        goToHomeBounds = new Rectangle(Constants.WORLD_WIDTH/2-150,Constants.WORLD_HEIGHT/6-100,300,100);
        continueZapperButtonBounds = new Rectangle();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("VikingHell.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 100;
        parameter.color= Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        font = generator.generateFont(parameter);
        parameter.size = 50;
        parameter.color = Color.BLACK;
        restartButtonFont = generator.generateFont(parameter);
        generator.dispose();

        FreeTypeFontGenerator generator1 = new FreeTypeFontGenerator(Gdx.files.internal("Quantify-Bold.ttf"));
        parameter.color = Color.WHITE;
        parameter.size = 30;
        goToHomeFont = generator1.generateFont(parameter);
        generator1.dispose();

        reset(playMode);
    }

    @Override
    public void render(float delta) {

        if(!stopped) {
            time += 2 * delta;
            timer += delta;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        extendViewport.apply();
        shapeRenderer.setProjectionMatrix(extendViewport.getCamera().combined);
        spriteBatch.setProjectionMatrix(extendViewport.getCamera().combined);

        if(!stopped) {
            if (timer >= 0.5) {
                timer -= 0.5;

                // dirty hack
                sw = -sw;
                if (sw == 1) {
                    rings.add(new CrossMeRing(shapeRenderer,playMode));
                } else {
                    NormalRing q = new NormalRing(shapeRenderer);
                    rings.add(q);
                    if (playBall == null) {
                        playBall = new PlayBall(shapeRenderer, q);
                    }
                }
            }
        }

        rings.begin();
        for (int i = 0; i < rings.size; i++) {
            if(rings.get(i).radius < 10){
                rings.removeIndex(i);
            }
        }
        rings.end();

        for (int i = rings.size-1 ; i >= 0 ; i--) {
            rings.get(i).render(delta);
        }

        for (ExplosionTriangle tri: explosionTriangles) {
            tri.render(delta);
        }

        explosionTriangles.begin();
        for (int i = 0; i < explosionTriangles.size ; i++) {
            if(explosionTriangles.get(i).getTime() > 8f){
                explosionTriangles.removeIndex(i);
            }
        }
        explosionTriangles.end();

        if(!stopped) {
            handleBall(delta);
        }

        score.render(delta);

        if (playBall!=null) {
            if (playBall.getRotateRadius() > 300) {
                stopped = true;
                gameOver = true;
                for (Ring r : rings) {
                    r.stop();
                }
                createExplosion();
                playBall = null;
            }

            else if (playBall.getRotateRadius() < 12) {
                stopped = true;
                gameOver = true;
                for(Ring r: rings){
                    r.stop();
                }
                createExplosion();
                playBall = null;

            }
        }


        if(!stopped) drawPauseButton();

        if(stopped) handleStoppedCase();

    }

    private void handleStoppedCase() {

        drawBanner();
        writeText("Go to Home",Constants.WORLD_WIDTH/2-150,Constants.WORLD_HEIGHT/8,goToHomeFont);

        if(gameOver) {
            handleGameOver();
        }
        else if(paused) {
            writeText("   Paused",Constants.WORLD_WIDTH/4.5f,Constants.WORLD_HEIGHT/2 + 50,font);
            drawButtonWithText("RESUME",0);
        }
    }

    private void handleGameOver() {
        writeText("Game Over",Constants.WORLD_WIDTH/4.5f,Constants.WORLD_HEIGHT/2 + 100,font);

        int zapperCount = Encrypt.decrypt(preferences.getString(Constants.ZAPPER_COUNT));
        if(chancesLeft <= 0) {
            writeText("No Chances Left",Constants.WORLD_WIDTH/4.5f,Constants.WORLD_HEIGHT/2,goToHomeFont);
            spriteBatch.begin();
            spriteBatch.draw(sadImage, 3 * Constants.WORLD_WIDTH / 4 - 40, Constants.WORLD_HEIGHT / 2 - 35, 40, 40);
            spriteBatch.end();
        }
        else if(zapperCount < 150){
            writeText("Not enough Zappers",Constants.WORLD_WIDTH/4.5f-50,Constants.WORLD_HEIGHT/2,goToHomeFont);
            spriteBatch.begin();
            spriteBatch.draw(sadImage, 3 * Constants.WORLD_WIDTH / 4 - 30, Constants.WORLD_HEIGHT / 2 - 40, 40, 40);
            spriteBatch.end();
        }
        else {
            writeText("Continue using 150 ", Constants.WORLD_WIDTH / 4.5f - 30, Constants.WORLD_HEIGHT / 2, goToHomeFont);
            continueZapperButtonBounds.set(Constants.WORLD_WIDTH / 4.5f - 30, Constants.WORLD_HEIGHT / 2 - 50, 800, 100);
            spriteBatch.begin();
            spriteBatch.draw(zapperImage, 3 * Constants.WORLD_WIDTH / 4 - 40, Constants.WORLD_HEIGHT / 2 - 40, 40, 40);
            spriteBatch.end();
        }
        drawButtonWithText("RESTART",-20);
    }

    private void drawPauseButton(){
        spriteBatch.begin();
        spriteBatch.draw(pauseButton,Constants.WORLD_WIDTH-60,Constants.WORLD_HEIGHT-60,40,40);
        spriteBatch.end();
    }

    private void drawButtonWithText(String text, int displacementY) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(Constants.WORLD_WIDTH/2-100,Constants.WORLD_HEIGHT/3-25+displacementY,200,50);
        shapeRenderer.end();

        bounds.set(Constants.WORLD_WIDTH/2-100,Constants.WORLD_HEIGHT/3-25+displacementY,200,50);

        spriteBatch.begin();
        restartButtonFont.draw(spriteBatch,text,Constants.WORLD_WIDTH/2-80,Constants.WORLD_HEIGHT/3+20 + displacementY);
        spriteBatch.end();

    }

    private void writeText(String text, float scrX, float scrY, BitmapFont font) {
        spriteBatch.begin();
        font.draw(spriteBatch,text,scrX,scrY);
        spriteBatch.end();
    }

    private void drawBanner() {

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bannerColor);
        shapeRenderer.rect(0,0,Constants.WORLD_WIDTH,3*Constants.WORLD_HEIGHT/4);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

    }

    private void handleBall(float delta) {

        if(playBall == null){
            Gdx.app.log("ball","null");
            return;
        }

        playBall.render(delta);

        if(playBall.isDetached()){

            for (int i = rings.size -1 ; i >= 0; i--) {

                Ring ring = rings.get(i);

                if(ring.equals(lastAttachedRing)){
                    continue;
                }

                float r = ring.radius;

                if(playBall == null){
                    Gdx.app.log("WTF","playball null");
                    return;
                }
                float diff = r - playBall.getRotateRadius();

               if(diff<7.5f && diff > -10){
                    if(ring.getClass().equals(NormalRing.class)){
                        playBall.setAttachedRing((NormalRing) ring);
                        score.increase();
                        break;
                    }
                    else {

                        CrossMeRing cmr = (CrossMeRing) ring;
                        float ballAngle = playBall.getAngle() ;

                        int arcCount = cmr.getArcNum();
                        float rot = cmr.getRot();


                        for (int j = 0; j < arcCount; j++) {

                            float q1 = 360/arcCount * j + rot;
                            float q2 = q1 + 180/arcCount  ;

                            float ballAngle1 = ballAngle;


                            if(ballAngle1 >= (q1-5) && ballAngle1 <= (q2+5) ){

                                createExplosion();

                                for(Ring mRing: rings){
                                    mRing.stop();
                                }
                                stopped = true;
                                gameOver = true;
                                playBall = null;
                            }

                            else if(ballAngle1+360 >= (q1-5) && ballAngle1+360 <= (q2+5)) {

                                createExplosion();

                                for(Ring mRing: rings){
                                    mRing.stop();
                                }

                                stopped = true;
                                gameOver = true;
                                playBall = null;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        //Gdx.app.log("width", String.valueOf(width));
        extendViewport.update(width,height,true);
    }


    private void createExplosion(){

        int n = MathUtils.random(20,30);
        for(int k=0;k< n;k++){
            ExplosionTriangle explosionTriangle = new ExplosionTriangle(shapeRenderer,playBall.getPosition(), k*360/n );
            explosionTriangles.add(explosionTriangle);
        }
        game.getPlayGameServices().submitScore(score.getScore(),playMode);
        Gdx.input.vibrate(500);

    }

    @Override
    public void pause() {
        paused =true;
        stopped = true;
        for (Ring r:rings){
            r.stop();
        }
    }

    @Override
    public void resume() {
        paused = false;
        if(gameOver!=true){
            stopped = false;
        }
        for (Ring r:rings){
            r.start();
        }
    }

    @Override
    public void hide() {

    }

    private void reset(int playMode){

        this.playMode = playMode;
        reset();
    }

    private void reset(){

        explosionTriangles.clear();
        rings.clear();
        time = 0;
        timer = 0;
        sw = 1;
        playBall = null;
        stopped = false;
        gameOver = false;
        score.reset();

        int playNum = preferences.getInteger(Constants.PLAY_COUNT);
        playNum++;
        Gdx.app.log("play_Count", String.valueOf(playNum));
        preferences.putInteger(Constants.PLAY_COUNT,playNum).flush();

        if(playNum >= 50){
            game.getPlayGameServices().unlockAchievement();
        }

        chancesLeft = 2;
    }



    @Override
    public void dispose() {
        spriteBatch.dispose();
        font.dispose();
        restartButtonFont.dispose();
        shapeRenderer.dispose();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        super.touchDown(screenX, screenY, pointer, button);

        Vector2 touchPos = extendViewport.unproject(new Vector2(screenX,screenY));

        if(gameOver){
            int zapperCount = Encrypt.decrypt(preferences.getString(Constants.ZAPPER_COUNT));
            if(zapperCount >= 150 && chancesLeft > 0) {
                if (continueZapperButtonBounds.contains(touchPos)) {
                    continueGameWithSameScore();
                }
            }
        }

        if(!paused){
            if(pauseButtonBounds.contains(touchPos)){
                pause();
                return true;
            }
        }


        if(stopped){
            if(bounds.contains(touchPos)){
                if(paused){
                    resume();
                }
                else if(gameOver) {
                    reset();
                }
            }
            else if(goToHomeBounds.contains(touchPos)){
                game.setHomeScreen();
            }
            return true;
        }


        if(playBall!=null) {
            lastAttachedRing = playBall.detach();
            Gdx.app.log("Hi", "touchDown");
        }

        return true;
    }

    private void continueGameWithSameScore() {
        explosionTriangles.clear();
        rings.clear();
        time = 0;
        timer = 0;
        sw = 1;
        playBall = null;
        stopped = false;
        gameOver = false;

        chancesLeft --;

        int currentZapperCount = Encrypt.decrypt(preferences.getString(Constants.ZAPPER_COUNT));
        currentZapperCount -= 150;
        preferences.putString(Constants.ZAPPER_COUNT,Encrypt.encrypt(currentZapperCount)).flush();

    }

    public boolean keyDown (int keycode) {

        if(keycode == Input.Keys.SPACE){
            if(playBall!=null) {
                lastAttachedRing = playBall.detach();
            }
        }

        if(keycode == Input.Keys.ESCAPE){
            if(!stopped){
                pause();
            }
        }

        if(keycode == Input.Keys.BACK){
            if(!stopped) {
                pause();
            }
        }

        return false;
    }

    public void setPlayMode(int mode) {
        playMode = mode;
    }
}
