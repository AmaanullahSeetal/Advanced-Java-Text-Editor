package texteditor;

import java.text.Normalizer;
import java.util.ArrayList;

public class FindPlugIn implements TextEditorPlugIn
{
	// CLASS FIELDS :
	private API api = null;
	
	// PLUGIN CONSTRUCTOR :
	public FindPlugIn() 
	{	
		// Do nothing...
	}

	// PLUGIN START METHOD :
	@Override
	public void start(API api) 
	{	
		this.api = api;
		
		// Add button to GUI :
		btnPressCallbk btnPressCallbk = new btnPressCallbk();	
		api.addButton("Find", btnPressCallbk);	
		
		FindEventCallbk fdEventCallbk = new FindEventCallbk();	
		api.addKeyPressCallbk("F3", fdEventCallbk);
		
		api.displayInListView("Find Plug-in");
	}
	
	
	// CALLBACK METHOD (KEY PRESS) :
	private class FindEventCallbk implements KeyPressCallbk_Interface
	{
		@Override
		public void notifyKeyPressHappened(KeyPressEvent_Interface keyPressEv)
		{
			// If function key F3 was pressed :
			if(keyPressEv.getKey().equals("F3"))
			{
				findText();
			}						
		}		
	}
	
	// CALLBACK METHOD (BUTTON PRESS) :
	private class btnPressCallbk implements BtnPressCallbk_Interface
	{
		@Override
		public void notifyBtnPressHappened(BtnPressEvent_Interface btnPressEv)
		{
			if(btnPressEv.getBtnName().equals("Find"))
			{
				findText();
			}
		}		
	}
	
	
	// DESCRIPTION : Normalise the editor text and search text and try to find a match, then highlights it in the original text.
	private void findText()
	{
		// NOTE: Due to the expansion of normalised texts due to ligatures, an array was
		//       made to map the index back to the original text for highlighting correctly.
		
		// Declare variables :
		String strToFind 				= null;
		String strToFind_n 				= null;
		String txt						= null;
		String subTxt					= null;
		String subTxt_n					= null;
		int caretIndx					= 0;
		int idx 						= 0;
		int totOffset 					= 0;
		int offset 						= 0;
		int startIdx					= 0;			 
		int endIdx						= 0;				
		int expandBy					= 0;
		ArrayList<Integer> offsetArr 	= new ArrayList<Integer>();
		
	
		// Prompt user for input string to find :
		strToFind = api.showDialog();				
		
		if(strToFind != null)
		{
			// Normalise the string to find text :
			strToFind_n = Normalizer.normalize(strToFind, Normalizer.Form.NFKC).toLowerCase();
			
			// Get Caret index :
			caretIndx = api.getCaretPosition();
			
			// Get text from editor and normalise it :
			txt 		= api.getText();
			subTxt 		= txt.substring(caretIndx, txt.length());
			subTxt_n 	= Normalizer.normalize(subTxt, Normalizer.Form.NFKC).toLowerCase();			
			
			// Get search string start index, else return -1 :
			idx = subTxt_n.indexOf(strToFind_n);						
			
			// Highlight text after Caret :
			if(idx != -1)
			{				
				// Scan through substring and build offsetting array :				
				for(char c : subTxt.toCharArray())
				{
					// Check if char is a ligature or not :
					offset = isLigature(c);
					
					// Add cummulative offset :
					offsetArr.add(totOffset);
					
					while(totOffset <  totOffset+offset)
					{
						// Increase incrementally till offset :
						totOffset++;						
						offsetArr.add(totOffset);									
						
						// Increase until offset(count) zeroes out :
						offset--;
					}					
				}
				
				// Adjust index to highlight the original text correctly :
				startIdx 	= idx + caretIndx - offsetArr.get(idx);				 
				endIdx 		= idx + caretIndx - offsetArr.get(idx) + strToFind_n.length();				
				expandBy	= offsetArr.get(idx + strToFind_n.length() - 1) - offsetArr.get(idx);
				
				api.highlightText(startIdx , endIdx - expandBy);
			}
			
		}
		
	}
	
	
	// DESCRIPTION : Detects whether a char is a ligature and returns the increase in chars when decomposed.
	private int isLigature(char inChar)
	{
		// Declare variables :
		String subTxt_n = null;
			
		// Check if char is a ligature :
		subTxt_n = Normalizer.normalize(Character.toString(inChar), Normalizer.Form.NFKC).toLowerCase();
		
		return subTxt_n.length()-1;
	}	
	
}
