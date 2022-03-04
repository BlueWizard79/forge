package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Selector;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.TransitionScreen;
import forge.util.NameGenerator;

import java.util.Random;

/**
 * NewGame scene that contains the character creation
 */
public class NewGameScene extends UIScene {
    TextField selectedName;
    Deck[] starterDeck;
    private Image avatarImage;
    private int avatarIndex = 0;
    private Selector race;
    private Selector deck;
    private Selector gender;
    private Selector difficulty;
    private ScrollPane scrollPane;
    private Label titleL, avatarL, nameL, raceL, genderL, difficultyL, deckL;
    private ImageButton leftArrow, rightArrow;
    private TextButton backButton, startButton;

    public NewGameScene() {
        super(Forge.isLandscapeMode() ? "ui/new_game_mobile.json" : "ui/new_game.json");
    }

    public boolean start() {
        if (selectedName.getText().isEmpty()) {
            selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
                WorldSave.generateNewWorld(selectedName.getText(),
                        gender.getCurrentIndex() == 0,
                        race.getCurrentIndex(),
                        avatarIndex,
                        deck.getCurrentIndex(),
                        Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()], 0);
                GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
                Forge.clearTransitionScreen();
                Forge.switchScene(SceneType.GameScene.instance);
            }
        };
        Forge.setTransitionScreen(new TransitionScreen(runnable, null, false, true));
        return true;
    }

    public boolean back() {
        Forge.switchScene(SceneType.StartScene.instance);
        return true;
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        selectedName = ui.findActor("nameField");
        selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        avatarImage = ui.findActor("avatarPreview");
        gender = ui.findActor("gender");
        gender.setTextList(new String[]{"Male", "Female"});
        gender.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                return NewGameScene.this.updateAvatar();
            }
        });
        Random rand = new Random();

        deck = ui.findActor("deck");

        starterDeck = Config.instance().starterDecks();
        Array<String> stringList = new Array<>(starterDeck.length);
        for (Deck deck : starterDeck)
            stringList.add(deck.getName());

        deck.setTextList(stringList);

        race = ui.findActor("race");
        race.addListener(new EventListener() {
            @Override
            public boolean handle(Event event) {
                return NewGameScene.this.updateAvatar();
            }
        });
        race.setTextList(HeroListData.getRaces());
        difficulty = ui.findActor("difficulty");

        Array<String> diffList = new Array<>(starterDeck.length);
        int i = 0;
        int startingDifficulty = 0;
        for (DifficultyData diff : Config.instance().getConfigData().difficulties) {
            if (diff.startingDifficulty)
                startingDifficulty = i;
            diffList.add(diff.name);
            i++;
        }
        difficulty.setTextList(diffList);
        difficulty.setCurrentIndex(startingDifficulty);
        avatarIndex = rand.nextInt();
        gender.setCurrentIndex(rand.nextInt());
        deck.setCurrentIndex(rand.nextInt());
        race.setCurrentIndex(rand.nextInt());
        ui.onButtonPress("back", new Runnable() {
            @Override
            public void run() {
                NewGameScene.this.back();
            }
        });
        ui.onButtonPress("start", new Runnable() {
            @Override
            public void run() {
                NewGameScene.this.start();
            }
        });
        ui.onButtonPress("leftAvatar", new Runnable() {
            @Override
            public void run() {
                NewGameScene.this.leftAvatar();
            }
        });
        ui.onButtonPress("rightAvatar", new Runnable() {
            @Override
            public void run() {
                NewGameScene.this.rightAvatar();
            }
        });

        scrollPane = ui.findActor("scroll");
        titleL = ui.findActor("titleL");
        avatarL = ui.findActor("avatarL");
        nameL = ui.findActor("nameL");
        raceL = ui.findActor("raceL");
        genderL = ui.findActor("genderL");
        difficultyL = ui.findActor("difficultyL");
        deckL = ui.findActor("deckL");
        leftArrow = ui.findActor("leftAvatar");
        rightArrow = ui.findActor("rightAvatar");
        backButton = ui.findActor("back");
        startButton = ui.findActor("start");
        if (!Forge.isLandscapeMode()) {
            float w = Scene.GetIntendedWidth();
            float sW = w - 20;
            float oX = w/2 - sW/2;
            float h = Scene.GetIntendedHeight();
            float sH = (h - 10)/12;

            selectedName.setWidth(280);
            selectedName.setX(w/2- selectedName.getWidth()/2);
            nameL.setFontScaleX(2);

            titleL.setFontScaleX(2);
            titleL.setX(selectedName.getX()-20);

            avatarImage.setScaleX(2);
            avatarImage.setX(w/2-avatarImage.getWidth());

            leftArrow.getImage().setScaleX(2);
            leftArrow.setX(selectedName.getX());
            rightArrow.getImage().setScaleX(2);
            rightArrow.setX(selectedName.getRight()-(rightArrow.getWidth()*2));

            avatarL.setFontScaleX(2);

            race.setScaleX(2);
            race.setWidth(140);
            race.setX(selectedName.getX());
            raceL.setFontScaleX(2);

            gender.setScaleX(2);
            gender.setWidth(140);
            gender.setX(selectedName.getX());
            genderL.setFontScaleX(2);

            difficulty.setScaleX(2);
            difficulty.setWidth(140);
            difficulty.setX(selectedName.getX());
            difficultyL.setFontScaleX(2);

            deck.setScaleX(2);
            deck.setWidth(140);
            deck.setX(selectedName.getX());
            deckL.setFontScaleX(2);

            scrollPane.setWidth(sW);
            scrollPane.setHeight(sH*11);
            scrollPane.setX(oX);

            float bW = w - 165;
            float bX = w/2 - bW/2;
            backButton.setWidth(bW/2);
            backButton.setHeight(20);
            backButton.setX(bX);
            startButton.setWidth(bW/2);
            startButton.setHeight(20);
            startButton.setX(backButton.getRight());

        }
        updateAvatar();
    }

    private void rightAvatar() {

        avatarIndex++;
        updateAvatar();
    }

    private void leftAvatar() {
        avatarIndex--;
        updateAvatar();
    }

    private boolean updateAvatar() {

        avatarImage.setDrawable(new TextureRegionDrawable(HeroListData.getAvatar(race.getCurrentIndex(), gender.getCurrentIndex() != 0, avatarIndex)));
        return false;
    }

    @Override
    public void create() {

    }

    @Override
    public void enter() {
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }
}
