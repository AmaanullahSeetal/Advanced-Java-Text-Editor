package texteditor;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import org.python.util.PythonInterpreter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.Window;

public class EditorGUI extends Application
{
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // CLASS FIELDS :
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~    
	private ObservableList<String> listViewList 						= FXCollections.observableArrayList();
    private TextArea textArea 											= new TextArea();
    private ToolBar toolBar 											= new ToolBar();
    private ArrayList<TxtChangedEvent_Interface> txtChangedEventList	= new ArrayList<>();
    private ArrayList<KeyPressEvent_Interface> keyPressEventList		= new ArrayList<>();
    private ResourceBundle bundle 										= null;
    private Locale locale		 										= null;    
    private ArrayList<keyMapCombn> keyCombsList							= null;
    
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // APPLICATION SETUP :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static void main(String[] args)
    {
        Application.launch(args);
    }
   
    
    @Override
    public void start(Stage stage)
    {
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
    	// STEP 1 : Setup ResourceBundle and Locale 
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    	
    	// Get the UI text for the default locale :
    	Locale locale			= Locale.getDefault();    	
    	ResourceBundle bundle 	= ResourceBundle.getBundle("bundle");
    	
    	// Extract CLI parameters (if any) :
        var localeString = getParameters().getNamed().get("locale");
    	
        if(localeString != null)
        {
        	// Get the specific bundle and locale :
        	locale 	= Locale.forLanguageTag(localeString);
        	bundle 	= ResourceBundle.getBundle("bundle", locale);
        }
        
        this.bundle = bundle;
        this.locale = locale;
        
        
        
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
        // STEP 2 : Setup GUI 
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        stage.setTitle(bundle.getString("EditorGUI_Window_Title"));
        stage.setMinWidth(800);

        // Create toolbar :
        Button LoadBtn 			= new Button(bundle.getString("EditorGUI_LoadFileBtn"));
        Button SaveBtn 			= new Button(bundle.getString("EditorGUI_SaveFileBtn"));
        Button plugInScriptBtn 	= new Button(bundle.getString("EditorGUI_AddPlugInScriptBtn"));
        toolBar.getItems().addAll(LoadBtn, SaveBtn, plugInScriptBtn);

        // Subtle user experience tweaks :
        toolBar.setFocusTraversable(false);
        toolBar.getItems().forEach(btn -> btn.setFocusTraversable(false));
        textArea.setStyle("-fx-font-family: 'monospace'");
        
        // Add the main parts of the UI to the window :
        BorderPane mainBox = new BorderPane();
        mainBox.setTop(toolBar);
        mainBox.setCenter(textArea);
        Scene scene = new Scene(mainBox);        
        
        // Button event handlers :
        LoadBtn.setOnAction(event -> loadFile(stage));
        SaveBtn.setOnAction(event -> saveFile(stage));
        plugInScriptBtn.setOnAction(event -> showDialogWithList(stage));   
                
        
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // STEP 3 : Read keymap file and generate a list of keyMapCombn object : 
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        try
	    {
        	String baseDirPath	= System.getProperty("user.dir");
        	Path path 			= Paths.get(baseDirPath + "/keymap");
        	
        	// Read keymap file :
            String keymapFile 	= new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            
            // Parse keymap file and generate a list of key combination objects :
            keysParser parser 	= new keysParser(new java.io.StringReader(keymapFile));
            this.keyCombsList 	= parser.Input(textArea);
	    }
	    catch(ParseException e)
	    {
	    	new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("keymap_ParseErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
	    }
	    catch(NoSuchFileException e)
	    {
	    	new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("keymap_NotFoundErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
	    } 
        catch (IOException e)
        {
        	new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("keymap_ReadErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		}
                
        
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // STEP 4 : Add event handlers for text changes
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        // Monitor TextArea text change & caret positioning :
        textArea.textProperty().addListener((ChangeListener<String>) (object, oldValue, newValue) -> 
        { 
			int caretIdx = textArea.getCaretPosition() + 1;
			
			// (SCRIPT EVENT HANDLER) Notify all observers of text changes :
		    for(TxtChangedEvent_Interface txtChangeEvent : txtChangedEventList)
		    {
		    	String detectTxt					= txtChangeEvent.getTextToDetect();
		    	TxtChangedCallbk_Interface cbEvent	= txtChangeEvent.getCallbkEvent();
		    	
		    	if( 	(caretIdx - detectTxt.length()) >= 0 
		    		&& 	(caretIdx - detectTxt.length()  < caretIdx) 
		    		&& 	(caretIdx <= textArea.getText().length()) )
		    	{	
		    		String typedStr = textArea.getText().substring(caretIdx-detectTxt.length(), caretIdx);		    		
		    		if(typedStr.equals(detectTxt))
		    		{
		    			cbEvent.notifyTxtChangeHappened(txtChangeEvent);
		    		}			    		
		    	}
		    }
		    
		});
        
        
        // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ 
        // STEP 5 : Add event handlers for key presses
    	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        
        // Monitor global keypress handler :
        scene.setOnKeyPressed(keyEvent -> 
        {
		    // Get key states :            
		    KeyCode key 	= keyEvent.getCode();
		    boolean ctrl 	= keyEvent.isControlDown();
		    boolean shift 	= keyEvent.isShiftDown();
		    boolean alt 	= keyEvent.isAltDown();		    
		    
		    // (KEYMAP EVENT HANDLER) Notify all observers of key presses :
		    if(keyCombsList != null)
		    {
		    	for(keyMapCombn keyCombn : keyCombsList)
			    {
			    	keyCombn.runKeyCombination(key, ctrl, shift, alt);
			    }
		    }
		        		
		    
		    // (PLUGIN EVENT HANDLER) Notify all observers of key presses :
    		for(KeyPressEvent_Interface event : keyPressEventList)
		    {
    			String cbKey 						= event.getKey();
    			KeyPressCallbk_Interface cbEvent	= event.getCallbkEvent();
    			
    			if(key.getName().equals(cbKey))
		    	{
    				cbEvent.notifyKeyPressHappened(event);
		    	}    			
		    }		    
		    
		});        
        
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOAD FILE :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void loadFile(Stage stage)
    {
    	// Declare variables :
    	FileChooser fileDialog 		= new FileChooser();
    	String encoding 			= null;
    	CharsetDecoder decoder 		= null;
    	InputStreamReader inStream	= null;
    	File file 					= null;
    	String line					= null;
    	
    	fileDialog.setTitle(bundle.getString("LoadFile_Window_Title"));
        
    	// Prompt user for a file to load :
        file = fileDialog.showOpenDialog(stage);
        
        if(file != null)
        {
        	// Prompt user for the encoding to use :
            encoding = getEncoding();            
           	
    		try 
    		{    			
    			if(encoding != null)
	            {    				
    				decoder 	= Charset.forName(encoding).newDecoder();    
    				inStream	= new InputStreamReader (new FileInputStream (file.getPath()),	decoder);
				
    				// Clear editor :
    				textArea.clear();
    				
    				try(BufferedReader buffRead = new BufferedReader(inStream) )
    		        {    		            
    		            while((line = buffRead.readLine()) != null)
    		            {
    		            	textArea.appendText(line + "\n");
    		            }
    		            
    		            // Flush buffer :
    		            buffRead.close();
    		        }
	            }
				 
			} 
    		catch (FileNotFoundException e) 
    		{
    			new Alert(	Alert.AlertType.ERROR,
							String.format(bundle.getString("LoadFile_NotFoundErr"), e.getMessage()),
							ButtonType.CLOSE).showAndWait();
			} 
    		catch (IOException e) 
    		{
    			new Alert(	Alert.AlertType.ERROR,
    						String.format(bundle.getString("LoadFile_ReadFileErr"), e.getMessage()),
    						ButtonType.CLOSE).showAndWait();
			}           
           
        }
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SAVE FILE :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void saveFile(Stage stage)
    {
    	// Declare variables :
    	FileChooser fileDialog 			= new FileChooser();
    	File file 						= null;
    	String encoding 				= null;
    	CharsetEncoder encoder			= null;
    	OutputStreamWriter outStream 	= null;
    	Scanner scanner 				= null;
    	
    	fileDialog.setTitle(bundle.getString("SaveFile_Window_Title"));
		
    	// Prompt user for a filename and directory to save :
    	file = fileDialog.showSaveDialog(stage);
    	
		if(file != null)
        {
			// Prompt user for an encoding :
			encoding = getEncoding();
			
			if(encoding != null) 
			{
				// Make an encoder based on the chosen encoding :
				encoder = Charset.forName(encoding).newEncoder(); 
				
				try 
				{
					outStream 	= new OutputStreamWriter(new FileOutputStream(file), encoder);
					scanner 	= new Scanner(textArea.getText());
					
					PrintWriter pw = new PrintWriter(outStream);
			        
					// Write each line to the text file :
		            while (scanner.hasNextLine()) 
		            {
		              String line = scanner.nextLine();
		              pw.print(line + "\n");	              
		            }
		            
		            // Flush scanner and printer :
		            scanner.close();
		            pw.close();			        					
				} 
				catch (FileNotFoundException e) 
				{
					new Alert(	Alert.AlertType.ERROR,
	    						String.format(bundle.getString("SaveFile_NotFoundErr"), e.getMessage()),
	    						ButtonType.CLOSE).showAndWait();
				} 
			}			
			  
        }
        
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // ENCODING DIALOG :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private String getEncoding()
    {
    	// Display a dialogbox and retrieve the encoding name : 
    	Dialog<String> encodingDialog 	= null;
    	String encoding					= null;
    	
    	// Setup the encoding dialogbox :
    	var encodingComboBox 	= new ComboBox<String>();
        var content 			= new FlowPane();
        encodingDialog 			= new Dialog<>();
        
        encodingDialog.setTitle(bundle.getString("Encoding_Window_Title"));
        encodingDialog.getDialogPane().setContent(content);
        encodingDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);        
        encodingDialog.setResultConverter(btn -> (btn == ButtonType.OK) ? encodingComboBox.getValue() : null);
        
        content.setHgap(8);
        content.getChildren().setAll(new Label(bundle.getString("Encoding_label")), encodingComboBox);            
        encodingComboBox.getItems().setAll("UTF-8", "UTF-16", "UTF-32");
        encodingComboBox.setValue("UTF-8");
        
    	// Prompt user for the encoding :
    	encoding = encodingDialog.showAndWait().orElse(null);
        
        return encoding;
    }
      
        
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // SEARCH TEXT DIALOG :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private String showFindDialog()
    {
        // Declare variables :
    	String inputStr = null;
        var dialog 		= new TextInputDialog();
        
        dialog.setTitle(bundle.getString("FindDialog_Window_Title"));
        dialog.setHeaderText(bundle.getString("FindDialog_HeaderTxt"));        
        
        inputStr = dialog.showAndWait().orElse(null);
        
        return inputStr;
    }
       
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOAD PLUGIN/SCRIPT DIALOG :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void showDialogWithList(Stage stage)
    {
    	// Declare variables :    	
        Button addPlugInBtn			= new Button(bundle.getString("dialogWithList_PlugInBtn"));
        Button addScriptBtn 		= new Button(bundle.getString("dialogWithList_ScriptBtn"));
        ToolBar toolBar 			= new ToolBar(addPlugInBtn, addScriptBtn);        
        ListView<String> listView 	= new ListView<>(listViewList); 
        BorderPane box 				= new BorderPane();
        Dialog dialog 				= new Dialog();
        
        // Setup dialogbox :
        addPlugInBtn.setOnAction	(event -> LoadPlugInFile(stage));
        addScriptBtn.setOnAction	(event -> LoadScriptFile(stage));        
        
        box.setTop(toolBar);
        box.setCenter(listView);
        
        dialog.setTitle(bundle.getString("dialogWithList_Window_Title"));
        dialog.setHeaderText(bundle.getString("dialogWithList_HeaderText"));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        
        // Show and prompt user :
        dialog.showAndWait();
    }
        
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOAD SCRIPTS :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void LoadScriptFile(Window dialogWindow)
    {
    	// Declare variables :
		FileChooser fileChoose 	= null;
		File file 				= null;
		String baseDir			= System.getProperty("user.dir");
		String initDir 			= baseDir + "/..";
		
		try 
		{			
			String resolvedDir = new File(initDir).getCanonicalPath();
			
			// Display FileChooser dialog :
			fileChoose = new FileChooser();
			fileChoose.setInitialDirectory(new File(resolvedDir));
			fileChoose.setTitle(bundle.getString("LoadScriptFile_Window_Title"));
			
			fileChoose.getExtensionFilters().addAll(
					 new ExtensionFilter("PYTHON Script", "*.py"),
					 new ExtensionFilter("All Files", "*.*"));
			
	        file = fileChoose.showOpenDialog(dialogWindow);    		
	        
	        if(file != null) 
	        {		        
        		// Load the script contents (using Jython) :
		        runScript(file);	        	     	
	        }
	     
		}
		catch (IOException e) 
		{
			new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("LoadScriptFile_IOErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		}
    	
    }

    private void runScript(File pythonFile)
    {
    	Platform.runLater(() -> 
		{
			// Variable declarations :				
			String pythonScript = null;
			
			try 
			{
				pythonScript = new String(Files.readAllBytes(pythonFile.toPath()), StandardCharsets.UTF_8);
				
				// Initialise the interpreter
				PythonInterpreter interpreter = new PythonInterpreter();
				
				// Instantiate new API object :
				txtEditorAPI scriptAPI = new txtEditorAPI();
				
				// Bind the API to the script environment :
				interpreter.set("api", scriptAPI);
							
				// Run the script :
				interpreter.exec(pythonScript);
			} 
			catch (IOException e) 
			{
				new Alert(	Alert.AlertType.ERROR,
							String.format(bundle.getString("runScript_ReadScriptErr"), e.getMessage()),
							ButtonType.CLOSE).showAndWait();
			}
				
		});
		
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // LOAD PLUGINS :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void LoadPlugInFile(Window dialogWindow)
    {
    	// Declare variables :
		FileChooser fileChoose 	= null;
		File file 				= null;		
		String baseDir			= System.getProperty("user.dir");
		String initDir 			= baseDir + "/..";
		
		try 
		{
			String resolvedDir = new File(initDir).getCanonicalPath();
			
			// Display FileChooser dialog :
			fileChoose = new FileChooser();
			fileChoose.setInitialDirectory(new File(resolvedDir));
			fileChoose.setTitle(bundle.getString("LoadPlugInFile_Window_Title"));
			
			fileChoose.getExtensionFilters().addAll(
					 new ExtensionFilter("JAVA Files", "*.class"),
					 new ExtensionFilter("All Files", "*.*"));
			
	        file = fileChoose.showOpenDialog(dialogWindow);   
	        
	        if(file != null) 
	        {
	        	// Load the plugin contents (using Reflection) :
		        runPlugin(file);
	        }	        
	        
		} 
		catch (IOException e) 
		{
			new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("LoadPlugInFile_IOErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		}
    	
    }
    
    private void runPlugin(File javaFile)
    {    	
    	// Variable declarations :
        String javaClassName 		= javaFile.getName().substring(0, javaFile.getName().length() - 6);
        Object rPluginObject 		= null;
        Class<?> rClass 			= null;
        Constructor rConstructor	= null;
        
    	try 
		{	
    		// Get plugin from its .class filename :
			rClass = Class.forName("texteditor." + javaClassName);			
			
			// Find its constructors (with no parameters) :
			rConstructor = rClass.getConstructor();
			
			// Instantiate plugin object :
			rPluginObject = rConstructor.newInstance();
	 		
	 		// Scan list of all methods to find the start method :
	 		for(Method rMethod : rClass.getDeclaredMethods())
			{
	 			if(rMethod.getName().equals("start"))
	 			{
	 				// Instantiate new API object :
	 				txtEditorAPI newAPIObj = new txtEditorAPI();
	 				
	 				// Call start method of plugin :
	 				rMethod.invoke(rPluginObject, newAPIObj);
	 			}	 			
			}
	        
		} 
		catch(ClassNotFoundException e) 
		{
			new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_ClassNotFoundErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (InstantiationException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_InstantiationErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (IllegalAccessException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_IllegalAccessErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (IllegalArgumentException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_IllegalArgumentErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (InvocationTargetException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_InvocationTargetErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (NoSuchMethodException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_NoSuchMethodErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		} 
    	catch (SecurityException e) 
    	{
    		new Alert(	Alert.AlertType.ERROR,
						String.format(bundle.getString("runPlugin_SecurityErr"), e.getMessage()),
						ButtonType.CLOSE).showAndWait();
		}
    }
    
        
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // APPLICATION API :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class txtEditorAPI implements API
    {
    	// Highlights text between the two indexes :
    	@Override    	
		public void highlightText(int startIndex, int endIndex) 
		{
    		Platform.runLater(() -> 
			{
				textArea.selectRange(startIndex, endIndex);
			});
		}

    	// Add a button to the toolbar :
		@Override
		public void addButton(String buttonName, BtnPressCallbk_Interface callbkEvent) 
		{
			Platform.runLater(() -> 
			{
				Button newBtn = new Button(buttonName);			
				toolBar.getItems().add(newBtn);
				
				BtnPressEvent btnEvent = new BtnPressEvent(callbkEvent, buttonName);
				
				newBtn.setOnAction(event -> callbkEvent.notifyBtnPressHappened(btnEvent));
				
			});			
		}

		// Show the find test dialogbox :
		@Override
		public String showDialog() 
		{
			return (showFindDialog());			
		}

		// Returns the current position of the caret :
		@Override
		public int getCaretPosition() 
		{
			return (textArea.getCaretPosition());			
		}
		
		// Set the position of the caret :
		@Override
		public void setCaretPosition(int caretPosition) 
		{
			Platform.runLater(() -> 
			{
				textArea.positionCaret(caretPosition);	
			});					
		}

		// Returns the text contents of the editor textArea :
		@Override
		public String getText() 
		{
			return (textArea.getText());
		}

		// Set the text contents of the editor textArea :
		@Override
		public void setText(String text) 
		{
			// REF : https://bugs.openjdk.java.net/browse/JDK-8081700
			Platform.runLater(() -> 
			{ 
				textArea.setText(text);
	        });
		}	
    	
		// Returns the active locale of the application : 
		@Override
		public Locale getLocale()
		{
			return locale;			
		}
		
		// Add a key press Event to trigger a callback when it occurs :
		@Override
		public void addKeyPressCallbk(String name, KeyPressCallbk_Interface eventKeyPress) 
		{
			KeyPressEvent cbEvent = new KeyPressEvent(eventKeyPress, name);
			keyPressEventList.add(cbEvent);
		}
		
		// Add a text change Event to trigger a callback when it occurs :
		@Override
		public void addTextChangeCallbk(String txtDetected, TxtChangedCallbk_Interface eventTxtChange)
		{
			TxtChangedEvent cbEvent = new TxtChangedEvent(eventTxtChange, txtDetected);
			txtChangedEventList.add(cbEvent);
		}

		// Add an item to the list of plugins/scripts :
		@Override
		public void displayInListView(String displayName) 
		{
	        // Add plugIn to listview :
	        listViewList.add(displayName);			
		}
		
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // KEY-PRESS EVENT (KEY PRESS) :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class KeyPressEvent implements KeyPressEvent_Interface
    {
    	// CLASS FIELDS :
    	KeyPressCallbk_Interface callback 	= null;
    	String key							= null;
    	
    	// CONSTRUCTOR :
		public KeyPressEvent(KeyPressCallbk_Interface callbkEvent, String key) 
		{
			this.callback 	= callbkEvent;
			this.key		= key;
		}

		// Gets the callback to trigger for this event :
		@Override
		public KeyPressCallbk_Interface getCallbkEvent() 
		{
			return callback;
		}

		// Gets the key name to identify the proper callback code :
		@Override
		public String getKey() 
		{
			return key;
		}
    }
    
    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // KEY-PRESS EVENT (TEXT CHANGE) :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class TxtChangedEvent implements TxtChangedEvent_Interface
    {
    	// CLASS FIELDS :
    	TxtChangedCallbk_Interface callback 	= null;
    	String txtDetected						= null;
    	
    	// CONSTRUCTOR :
		public TxtChangedEvent(TxtChangedCallbk_Interface callbkEvent, String key) 
		{
			this.callback 		= callbkEvent;
			this.txtDetected	= key;
		}
		
		// Gets the callback to trigger for this event :
		@Override
		public TxtChangedCallbk_Interface getCallbkEvent() 
		{
			return callback;
		}
		
		// Gets the text to find to identify the proper callback code :
		@Override
		public String getTextToDetect() 
		{
			return txtDetected;
		}
    }

    
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // KEY-PRESS EVENT (BUTTON PRESS) :
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private class BtnPressEvent implements BtnPressEvent_Interface
    {
    	// CLASS FIELDS :
    	BtnPressCallbk_Interface callback 	= null;
    	String btnName						= null;
    	
    	// CONSTRUCTOR :
		public BtnPressEvent(BtnPressCallbk_Interface callbkEvent, String btnName) 
		{
			this.callback 	= callbkEvent;
			this.btnName	= btnName;
		}

		// Gets the callback to trigger for this event :
		@Override
		public BtnPressCallbk_Interface getCallbkEvent() 
		{

			return callback;
		}

		// Gets the button name to identify the proper callback code :
		@Override
		public String getBtnName() 
		{
			return btnName;
		}
    }
}
