package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import forge.Forge;
import forge.adventure.util.Controls;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;
import forge.screens.TransitionScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Scene to load and save the game.
 */
public class SaveLoadScene extends UIScene {
    private final IntMap<TextButton> buttons = new IntMap<>();
    IntMap<WorldSaveHeader> previews = new IntMap<>();
    Color defColor;
    Table layout;
    boolean save = true;
    Dialog dialog;
    TextField textInput;
    Label header;
    int currentSlot = -3, lastSelectedSlot = 0;
    Image previewImage;
    Image previewBorder;
    TextButton saveLoadButton, back;
    TextButton quickSave;
    TextButton autoSave;
    boolean init;

    public SaveLoadScene() {
        super(Forge.isLandscapeMode() ? "ui/save_load_mobile.json" : "ui/save_load.json");
    }


    private TextButton addSaveSlot(String name, int i) {
        layout.add(Controls.newLabel(name)).align(Align.left).pad(2, 5, 2, 10);
        TextButton button = Controls.newTextButton("...");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    if (!button.isDisabled())
                        select(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        layout.add(button).align(Align.left).expandX();
        buttons.put(i, button);
        layout.row();
        return button;

    }

    public void back() {
        Forge.switchToLast();
    }

    public boolean select(int slot) {
        currentSlot = slot;
        if (slot > 0)
            lastSelectedSlot = slot;
        if (previews.containsKey(slot)) {
            WorldSaveHeader header = previews.get(slot);
            if (header.preview != null) {
                previewImage.setDrawable(new TextureRegionDrawable(new Texture(header.preview)));
                previewImage.layout();
                previewImage.setVisible(true);
            }
        } else {
            if (previewImage != null)
                previewImage.setVisible(false);
        }
        for (IntMap.Entry<TextButton> butt : new IntMap.Entries<TextButton>(buttons)) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextButton button = buttons.get(slot);
            button.setColor(Color.RED);
        }

        return true;
    }

    public void loadSave() {
        if (save) {
            if (currentSlot > 0) {
                //prevent NPE, allowed saveslot is 1 to 10..
                textInput.setText(buttons.get(currentSlot).getText().toString());
                dialog.show(stage);
                stage.setKeyboardFocus(textInput);
            }
        } else {
            if (WorldSave.load(currentSlot)) {
                Forge.setTransitionScreen(new TransitionScreen(new Runnable() {
                    @Override
                    public void run() {
                        Forge.switchScene(SceneType.GameScene.instance);
                    }
                }, null, false, true));
            } else {
                Forge.clearTransitionScreen();
            }
        }
    }

    public boolean saveAbort() {

        dialog.hide();
        return true;
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }

    public void save() {
        dialog.hide();
        if (WorldSave.getCurrentSave().save(textInput.getText(), currentSlot)) {
            updateFiles();
            Forge.switchScene(SceneType.GameScene.instance);
        }


    }

    private void updateFiles() {

        File f = new File(WorldSave.getSaveDir());
        f.mkdirs();
        File[] names = f.listFiles();
        if (names == null)
            throw new RuntimeException("Can not find save directory");
        previews.clear();
        for (File name : names) {
            if (WorldSave.isSafeFile(name.getName())) {
                try {

                    try (FileInputStream fos = new FileInputStream(name.getAbsolutePath());
                         InflaterInputStream inf = new InflaterInputStream(fos);
                         ObjectInputStream oos = new ObjectInputStream(inf)) {


                        int slot = WorldSave.filenameToSlot(name.getName());
                        WorldSaveHeader header = (WorldSaveHeader) oos.readObject();
                        buttons.get(slot).setText(header.name);
                        previews.put(slot, header);
                    }

                } catch (ClassNotFoundException | IOException | GdxRuntimeException e) {


                }
            }
        }

    }


    public void setSaveGame(boolean save) {
        if (save) {
            header.setText(Forge.getLocalizer().getMessage("lblSaveGame"));
            saveLoadButton.setText(Forge.getLocalizer().getMessage("lblSave"));
        } else {
            header.setText(Forge.getLocalizer().getMessage("lblLoadGame"));
            saveLoadButton.setText(Forge.getLocalizer().getMessage("lblLoad"));
        }
        autoSave.setDisabled(save);
        quickSave.setDisabled(save);
        this.save = save;
    }

    @Override
    public void enter() {
        if (lastSelectedSlot > 0)
            select(lastSelectedSlot);
        else
            select(-3);
        updateFiles();
        autoSave.getLabel().setText(Forge.getLocalizer().getMessage("lblAutoSave"));
        quickSave.getLabel().setText(Forge.getLocalizer().getMessage("lblQuickSave"));
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        if (!this.init) {
            layout = new Table();
            stage.addActor(layout);
            dialog = Controls.newDialog(Forge.getLocalizer().getMessage("lblSave"));
            textInput = Controls.newTextField("");
            if (!Forge.isLandscapeMode()) {
                dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2).pad(2, 15, 2, 15);
                dialog.getButtonTable().row();
                dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName")+": ")).align(Align.left).pad(2, 15, 2, 2);
                dialog.getButtonTable().add(textInput).fillX().expandX().padRight(15);
                dialog.getButtonTable().row();
                dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblSave"), new Runnable() {
                    @Override
                    public void run() {
                        SaveLoadScene.this.save();
                    }
                })).align(Align.left).padLeft(15);
                dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbort"), new Runnable() {
                    @Override
                    public void run() {
                        SaveLoadScene.this.saveAbort();
                    }
                })).align(Align.right).padRight(15);
            } else {
                dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2);
                dialog.getButtonTable().row();
                dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName")+": ")).align(Align.left);
                dialog.getButtonTable().add(textInput).fillX().expandX();
                dialog.getButtonTable().row();
                dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblSave"), new Runnable() {
                    @Override
                    public void run() {
                        SaveLoadScene.this.save();
                    }
                })).align(Align.left);
                dialog.getButtonTable().add(Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbort"), new Runnable() {
                    @Override
                    public void run() {
                        SaveLoadScene.this.saveAbort();
                    }
                })).align(Align.right);
            }
            previewImage = ui.findActor("preview");
            previewBorder = ui.findActor("preview_border");
            header = Controls.newLabel(Forge.getLocalizer().getMessage("lblSave"));
            header.setAlignment(Align.center);
            layout.add(header).pad(2).colspan(4).align(Align.center).expandX();
            layout.row();
            autoSave = addSaveSlot(Forge.getLocalizer().getMessage("lblAutoSave"), WorldSave.AUTO_SAVE_SLOT);
            quickSave = addSaveSlot(Forge.getLocalizer().getMessage("lblQuickSave"), WorldSave.QUICK_SAVE_SLOT);
            for (int i = 1; i < 11; i++)
                addSaveSlot(Forge.getLocalizer().getMessage("lblSlot")+": " + i, i);

            saveLoadButton = ui.findActor("save");
            saveLoadButton.getLabel().setText(Forge.getLocalizer().getMessage("lblSave"));
            ui.onButtonPress("save", new Runnable() {
                @Override
                public void run() {
                    SaveLoadScene.this.loadSave();
                }
            });
            back = ui.findActor("return");
            back.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
            ui.onButtonPress("return", new Runnable() {
                @Override
                public void run() {
                    SaveLoadScene.this.back();
                }
            });

            defColor = saveLoadButton.getColor();

            ScrollPane scrollPane = ui.findActor("saveSlots");
            scrollPane.setActor(layout);
            if (!Forge.isLandscapeMode()) {
                float w = Scene.GetIntendedWidth();
                float sW = w - 20;
                float oX = w/2 - sW/2;
                float h = Scene.GetIntendedHeight();
                float sH = (h - 10)/12;
                scrollPane.setWidth(sW);
                scrollPane.setHeight(sH*11);
                scrollPane.setX(oX);
                previewImage.setScale(1, 1.2f);
                previewImage.setX(scrollPane.getRight()-105);
                previewImage.setY(scrollPane.getTop()-71);
                float bW = w - 165;
                float bX = w/2 - bW/2;
                back.setWidth(bW/2);
                back.setHeight(20);
                back.setX(bX);
                saveLoadButton.setWidth(bW/2);
                saveLoadButton.setHeight(20);
                saveLoadButton.setX(back.getRight());
            }
            this.init = true;
        }
    }
}
