package texteditor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class DatePlugIn implements TextEditorPlugIn
{
	// CLASS FIELDS :
	private API api = null;
	
	
	// PLUGIN CONSTRUCTOR :
	public DatePlugIn()
	{
		// Do nothing...
	}
	
	
	// PLUGIN START METHOD :
	@Override
	public void start(API api) 
	{
		this.api = api;
		
		// Add button to GUI :
		insertDateEvent insDateEvent = new insertDateEvent();
		api.addButton("Date", insDateEvent);
		api.displayInListView("Date Plug-in");
	}

	
	// CALLBACK METHOD (BUTTON PRESS) :
	private class insertDateEvent implements BtnPressCallbk_Interface
	{
		@Override
		public void notifyBtnPressHappened(BtnPressEvent_Interface btnPressEv)
		{		
			// If the button pressed is, insert the date :
			if(btnPressEv.getBtnName().equals("Date"))
			{
				insertDate();
			}	
		}		
	}
	
	// DESCRIPTION : Insert the date at the caret position (with the correct locale formatting)
	private void insertDate()
	{
		// Declare variables :
		int caretIndx 					= 0;
		String editorTxt 				= null;
		Locale locale					= null;
		LocalDateTime localDateTime 	= null;
		ZonedDateTime zonedDateTime 	= null;
		DateTimeFormatter dateFormater 	= null;
		String formattedDate			= null;
		String newStr 					= null;
				
		// Get Caret index :
		caretIndx = api.getCaretPosition();
		
		// Get text from editor :
		editorTxt = api.getText();
		
		// Get date and time for locale :
		locale			= api.getLocale();    	
    	localDateTime 	= LocalDateTime.now();
    	zonedDateTime 	= ZonedDateTime.of(localDateTime, ZoneId.of("GMT"));
    	dateFormater 	= DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
    	
    	// Format the date :
    	formattedDate = dateFormater.format(zonedDateTime);		    	
    	
    	// Insert date in editor's text :
    	newStr = new StringBuilder(editorTxt).insert(caretIndx, formattedDate).toString();
		api.setText(newStr);	
		
		// Place caret back :
		api.setCaretPosition(caretIndx);
	}
}
