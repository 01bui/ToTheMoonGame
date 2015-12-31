package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

public class PhysicTown extends SimpleApplication implements ActionListener, AnimEventListener, AnalogListener {

    private PhysicTown PhysicTown;
    private BulletAppState bulletAppState;
    private RigidBodyControl scenePhy;
    private AnimControl controlMonkey;
    private AnimChannel channelMonkey;
    private static final String ANI_IDLE = "Idle";
    private static final String ANI_WALK = "Walk";
    private AnimControl controlDoctor;
    private AnimChannel channelDoctor;
    private static final String ANIM_IDLE = "idle";
    private static final String ANIM_WALK = "walk";
    private Node sceneNode;
    private Node playerNode;
    private Node doctorNode;
    private Node monkeyNode;
    private BetterCharacterControl playerControl; 
    private BetterCharacterControl monkeyControl;
    private BetterCharacterControl doctorControl;
    private Vector3f walkDirection = new Vector3f(0,0,0);
    private Vector3f monkeyWalkDirection = new Vector3f(0,0,0);
    private Vector3f viewDirection = new Vector3f(0,0,1);
    private boolean rotateLeft = false, rotateRight = false, FOUND = false,
                    strafeLeft = false, strafeRight = false,
                    forward    = false, backward    = false;
    private float speed = 8;
    private CameraNode camNode;
    private float distance = 0;
    private BitmapText distanceText;
    private BitmapText Text;
    private BitmapText MainText;
    private Picture logo;
    private AudioNode footStepAudio;
    private FilterPostProcessor fpp;
    private LightScatteringFilter sunLightFilter;
    // global vector where the sun is on the skybox
    private Vector3f lightDir = new Vector3f(-0.39f, -0.32f, -0.74f);

    public static void main(String[] args) {
        PhysicTown app = new PhysicTown();
        app.start();
    }

    public void simpleInitApp() {
        initPhysics();
        initLight();
        initNavigation();
        initScene();
        initCharacter();
        initCamera();
        //GUIText for the quest
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        MainText = new BitmapText(guiFont);
        MainText.setSize(guiFont.getCharSet().getRenderedSize());
        MainText.move(settings.getWidth()/2 -300, settings.getHeight(), 0);
        MainText.setText("Quest 1: Find the monkey statue and bring it to the doctor");
        MainText.setColor(ColorRGBA.Red);
        guiNode.attachChild(MainText);
        
        //GUIText for the distance to the doctor
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        distanceText = new BitmapText(guiFont);
        distanceText.setSize(guiFont.getCharSet().getRenderedSize());
        distanceText.move(settings.getWidth()/2+50, distanceText.getLineHeight()+20, 0);
        distanceText.setColor(ColorRGBA.Blue);
        //distanceText.setSize(2);
        guiNode.attachChild(distanceText);
                
        // Display a 2D image or icon on depth layer -2
        Picture frame = new Picture("User interface frame");
        frame.setImage(assetManager, "Interface/frame.png", false);
        frame.move(settings.getWidth() / 2 - 265, 0, -2);
        frame.setWidth(530);
        frame.setHeight(10);
        guiNode.attachChild(frame);

        // Display a 2D image or icon on depth layer -1
        logo = new Picture("logo");
        logo.setImage(assetManager, "Interface/chimpanzee-sad.gif", true);
        logo.move(settings.getWidth() / 2 - 47, 2, -1);
        logo.setWidth(46);
        logo.setHeight(38);
        guiNode.attachChild(logo);
        
        //Backgournd Sound
        
        AudioNode natureAudio = new AudioNode(assetManager, "Sounds/River.ogg");
        natureAudio.setVolume(1);
        natureAudio.setLooping(true);
        natureAudio.play(); 
        
        //Positional Sound
        Node riverbedNode = new Node("Riverbed");
        riverbedNode.setLocalTranslation(Vector3f.ZERO);
        rootNode.attachChild(riverbedNode);
        
        AudioNode riverAudio = new AudioNode(assetManager, "Sounds/River.ogg");
        riverAudio.setPositional(true);
        riverAudio.setRefDistance(10f);
        riverAudio.setMaxDistance(1000f);
        riverAudio.setVolume(1);
        riverAudio.setLooping(true);
        riverbedNode.attachChild(riverAudio);
        riverAudio.play();
        
        footStepAudio = new AudioNode(assetManager, "Sounds/Footsteps.ogg");
        
                
        //rootNode.setShadowMode(RenderQueue.ShadowMode.Off); // reset first
        //playerNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //doctorNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //monkeyNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //sceneNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
    }

    /** Initialize the physics simulation */
    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true); // collision shapes visible
    }
    
        /** An ambient light and a directional sun light */
    private void initLight() {
        AmbientLight ambient = new AmbientLight();
        rootNode.addLight(ambient);
        // make light shine from where sun is on skybox
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        rootNode.addLight(sun);    
        // init the filter postprocessor
        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        // make light beams appear from where sun is on skybox
        sunLightFilter = new LightScatteringFilter(lightDir.mult(-3000));
        fpp.addFilter(sunLightFilter);
        
        DirectionalLightShadowRenderer dlsr = 
                new DirectionalLightShadowRenderer(assetManager, 1024, 2);
        dlsr.setLight(sun);
        viewPort.addProcessor(dlsr);

        fpp = new FilterPostProcessor(assetManager);

        DirectionalLightShadowFilter dlsf = 
                new DirectionalLightShadowFilter(assetManager, 1024, 2);
        dlsf.setLight(sun);
        dlsf.setEnabled(true); // try true or false
        fpp.addFilter(dlsf);
        viewPort.addProcessor(fpp);
        
    }

    private void initCharacter() {
        // 1. Create a player node.
        playerNode = new Node("the player"); 
        playerNode.setLocalTranslation(new Vector3f(50, 6, -50));
        rootNode.attachChild(playerNode);
        playerControl = new BetterCharacterControl(1.5f, 4, 30f);
        playerControl.setJumpForce(new Vector3f(0, 300, 0));
        playerControl.setGravity(new Vector3f(0, -10, 0));
        playerNode.addControl(playerControl);
        bulletAppState.getPhysicsSpace().add(playerControl);
        
        // 2. Create a monkey node.
        monkeyNode = (Node) assetManager.loadModel("Models/monkeyExport/Jaime.j3o");
        monkeyNode.scale(2); 
        monkeyNode.setLocalTranslation(new Vector3f(0, 0, 0)); 
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");        
        TextureKey monkeyDiffuse = new TextureKey("Models/monkeyExport/diffuseMap.jpg", false);
        mat.setTexture("DiffuseMap",assetManager.loadTexture(monkeyDiffuse));
        TangentBinormalGenerator.generate(monkeyNode);
        TextureKey monkeyNormal = new TextureKey("Models/monkeyExport/NormalMap.png", false);
        mat.setTexture("NormalMap",assetManager.loadTexture(monkeyNormal));
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Gray);
        mat.setColor("Diffuse", ColorRGBA.White); 
        monkeyNode.setMaterial(mat);
        rootNode.attachChild(monkeyNode);
        
        controlMonkey = monkeyNode.getControl(AnimControl.class);
        controlMonkey.addListener(this);
        channelMonkey = controlMonkey.createChannel();
        channelMonkey.setAnim(ANI_IDLE);
        monkeyControl = new BetterCharacterControl(1.5f, 4, 30f);
        monkeyControl.setJumpForce(new Vector3f(0, 300, 0));
        monkeyControl.setGravity(new Vector3f(0, -10, 0));
        monkeyNode.addControl(monkeyControl);
        bulletAppState.getPhysicsSpace().add(monkeyControl);
        
        // 3. Create a doctor node.
        doctorNode = (Node) assetManager.loadModel("Models/malebuilder_male.j3o");
        doctorNode.setLocalTranslation(new Vector3f(30, 0, -5)); 
        doctorNode.scale(3); 
        rootNode.attachChild(doctorNode);
        
        controlDoctor = doctorNode.getControl(AnimControl.class);
        controlDoctor.addListener(this);
        channelDoctor = controlDoctor.createChannel();
        channelDoctor.setAnim(ANIM_IDLE);
        doctorControl = new BetterCharacterControl(1.5f, 4, 30f);
        doctorControl.setJumpForce(new Vector3f(0, 300, 0));
        doctorControl.setGravity(new Vector3f(0, -10, 0));
        doctorNode.addControl(doctorControl);
        bulletAppState.getPhysicsSpace().add(doctorControl);

    }

    /** CameraNode depends on playerNode. The camera follows the player. */
    private void initCamera() {
        camNode = new CameraNode("CamNode", cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        camNode.setLocalTranslation(new Vector3f(0, 4, -6));
        Quaternion quat = new Quaternion();
        quat.lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y);
        camNode.setLocalRotation(quat);
        playerNode.attachChild(camNode);
        camNode.setEnabled(true);
        flyCam.setEnabled(false);
    }

    /** Load a model with floors and walls and make them solid. */
    private void initScene() {
        // make the sky blue
        //rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false)); 
        // 1. Load the scene node
        assetManager.registerLocator("town.zip", ZipLocator.class);
        sceneNode = (Node) assetManager.loadModel("main.scene");
        sceneNode.scale(1.5f);
        rootNode.attachChild(sceneNode);
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));
        // 2. Create a RigidBody PhysicsControl with mass zero
        // 3. Add the scene's PhysicsControl to the scene's geometry
        // 4. Add the scene's PhysicsControl to the PhysicsSpace
        scenePhy = new RigidBodyControl(0f);
        sceneNode.addControl(scenePhy);
        bulletAppState.getPhysicsSpace().add(scenePhy);
    }


    /**
     * We override default fly camera key mappings (WASD), because we want to
     * use them for physics-controlled walking and jumping of the player.
     */
    private void initNavigation() {
        flyCam.setMoveSpeed(100);
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back",    new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Rotate Left",  new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Rotate Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Strafe Left",  new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Strafe Right", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Follow", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Move", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(this, "Forward", "Rotate Left", "Rotate Right", "Follow", "Move");
        inputManager.addListener(this, "Back", "Strafe Right", "Strafe Left", "Jump");
    }
        
    public Vector3f getLoc() {
        return playerNode.getLocalTranslation();
    }

    /**
     * Our  custom navigation actions are triggered by user input (WASD). 
     * No walking happens here yet -- we only keep track of 
     * the direction the user wants to go.
     */
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("Rotate Left")) {
            rotateLeft = isPressed;
        } else if (name.equals("Rotate Right")) {
            rotateRight = isPressed;
        } else  if (name.equals("Strafe Left")) {
            strafeLeft = isPressed;
        } else if (name.equals("Strafe Right")) {
            strafeRight = isPressed;
        } else if (name.equals("Forward")) {
            forward = isPressed;
            footStepAudio.play();
        } else if (name.equals("Back")) {
            backward = isPressed;
        } else if (name.equals("Jump")) {
            playerControl.jump();
        } else if (name.equals("Follow") && isPressed ) {
                
                //follow = isPressed;
                System.out.println("Left Click detected: ");
                //If yes, take the location of where the cursor is pointed at in the screen.
                CollisionResults results = new CollisionResults(); 
                Vector2f click2d = inputManager.getCursorPosition();
                Vector3f click3d = cam.getWorldCoordinates(new Vector2f(click2d.getX(), click2d.getY()), 0f);
                Vector3f dir     = cam.getWorldCoordinates( new Vector2f(click2d.getX(), click2d.getY()), 1f).subtractLocal(click3d);
                //Aim the ray staring from the click location into the calculated forward direction
                Ray ray = new Ray(click3d, dir);
                monkeyNode.collideWith(ray, results);
                
                               
                //If the user has clicked anything, the results list is not EMPTY
                if (results.size()>0){
                    System.out.println("Target Found");
                    FOUND = true;
                    if (!channelMonkey.getAnimationName().equals(ANI_WALK)) {
                        channelMonkey.setAnim(ANI_WALK);
                    }
                    //GUIText when found the monkey statue
                    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
                    Text = new BitmapText(guiFont);
                    Text.setSize(guiFont.getCharSet().getRenderedSize());
                    Text.move(settings.getWidth() / 2 - 50, Text.getLineHeight() + 40, 0);
                    Text.setText("You found the monkey statue. Bring it to the doctor");
                    Text.setColor(ColorRGBA.Red);
                    guiNode.attachChild(Text);
                    //Put the monkey in the inventory
                    monkeyNode.removeFromParent();                    
                }
                }
        else if (name.equals("Move") && isPressed ) {
            if (!channelDoctor.getAnimationName().equals(ANIM_WALK)) {
                    channelDoctor.setAnim(ANIM_WALK);
                }
        }
        if (name.equals("Move") && !isPressed) {
                channelDoctor.setAnim(ANIM_IDLE);
        }
    };
    
     public void onAnalog(String name, float intensity, float tpf) {
            if (name.equals("Move")) {
                doctorNode.move(0, 0, tpf);
            }
    };
    
    
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals(ANI_WALK)) {
            System.out.println(control.getSpatial().getName() + " completed one walk loop.");
        } else if (animName.equals(ANI_IDLE)) {
            System.out.println(control.getSpatial().getName() + " completed one idle loop.");
        }
        
        if (animName.equals(ANIM_WALK)) {
            System.out.println(control.getSpatial().getName() + " completed one walk loop.");
        } else if (animName.equals(ANIM_IDLE)) {
            System.out.println(control.getSpatial().getName() + " completed one idle loop.");
        }
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals(ANI_WALK)) {
            System.out.println(control.getSpatial().getName() + " started walking.");
        } else if (animName.equals(ANI_IDLE)) {
            System.out.println(control.getSpatial().getName() + " started being idle.");
        }
        
        if (animName.equals(ANIM_WALK)) {
            System.out.println(control.getSpatial().getName() + " started walking.");
        } else if (animName.equals(ANIM_IDLE)) {
            System.out.println(control.getSpatial().getName() + " started being idle.");
        }
    }

    /**
     * First-person walking happens here in the update loop.
     */
    @Override
    public void simpleUpdate(float tpf) {
 
        // Get current forward and left vectors of the playerNode: 
        Vector3f modelForwardDir = playerNode.getWorldRotation().mult(Vector3f.UNIT_Z);
        Vector3f modelLeftDir    = playerNode.getWorldRotation().mult(Vector3f.UNIT_X);
        // Depending on which nav keys are pressed, determine the change in direction

        walkDirection.set(0, 0, 0);
        if (strafeLeft) {
            walkDirection.addLocal(modelLeftDir.mult(speed));
        } else if (strafeRight) {
            walkDirection.addLocal(modelLeftDir.mult(speed).negate());
        }
        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(speed));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.mult(speed).negate());
        }
        playerControl.setWalkDirection(walkDirection);
        // Depending on which nav keys are pressed, determine the change in rotation
        if (rotateLeft) {
            Quaternion rotateL = new Quaternion().fromAngleAxis(FastMath.PI * tpf, Vector3f.UNIT_Y);
            rotateL.multLocal(viewDirection);
        } else if (rotateRight) {
            Quaternion rotateR = new Quaternion().fromAngleAxis(-FastMath.PI * tpf, Vector3f.UNIT_Y);
            rotateR.multLocal(viewDirection);
        }
        playerControl.setViewDirection(viewDirection);
        
        //GUIText
        distance = playerNode.getLocalTranslation().distance(doctorNode.getLocalTranslation());
        distanceText.setText("Distance to the doctor: " + distance); // update the display

        // change the GUI icon depending on distance to the center of the scene
        if (distance < 10f && FOUND == true) {
            logo.setImage(assetManager, "Interface/chimpanzee-smile.gif", true);
            Text.removeFromParent();
            distanceText.setText("Congratulations! You finished Quest 1");
            distanceText.setColor(ColorRGBA.Red);
        } else {
            logo.setImage(assetManager, "Interface/chimpanzee-sad.gif", true);
        }
    }
}
