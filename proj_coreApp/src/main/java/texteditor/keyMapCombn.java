package texteditor;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;

public class keyMapCombn 
{
	// CLASS FIELDS :
	String strToInsert		= null;	
	String alt 				= null;
	String ctrl 			= null;
	String shift 			= null;
	String vName 			= null;
	String insPos			= null;
	String task				= null;	
	boolean isAltPressed 	= false;
	boolean isCtrlPressed 	= false;
	boolean isShiftPressed 	= false;
	KeyCode code			= null;	
	TextArea textArea		= null;
	
	// CONSTRUCTOR :
	public keyMapCombn(Token str, Token alt, Token ctrl, Token shift, Token vName, String insPos, Token task, TextArea textArea)
	{
		this.strToInsert 	= str.image.substring(1, str.image.length()-1);
		this.alt 			= alt.image;
		this.ctrl 			= ctrl.image;
		this.shift 			= shift.image;
		this.vName 			= vName.image;
		this.insPos 		= insPos;
		this.task 			= task.image;
		this.isAltPressed 	= (alt.image 	!= null);
		this.isCtrlPressed 	= (ctrl.image 	!= null);
		this.isShiftPressed = (shift.image 	!= null);
		this.code 			= KeyCode.valueOf(vName.image.toUpperCase());
		this.textArea		= textArea;
	}
	
	// DESCRIPTION : Triggers the relevant method based on which key modifier combination is pressed :
	public void runKeyCombination(KeyCode key, boolean ctrl, boolean shift, boolean alt )
	{		
		if((isAltPressed == alt) && (isCtrlPressed == ctrl) && (isShiftPressed == shift) && (key == code))
		{			
			switch(task+insPos)
			{
				case "insert"+"@start":
					insertTextAtStart();
				break;
				
				case "insert"+"@caret":
					insertTextAtCaret();
				break;
				
				case "delete"+"@start":
					deleteTextAtStart();
				break;
				
				case "delete"+"@caret":
					deleteTextAtCaret();
				break;
			}
		}
	}
		
	// DESCRIPTION : Insert a string at the start of the line :
	private void insertTextAtStart()
	{
		// Declare variables :
		int caretIdx 			= textArea.getCaretPosition();
		boolean notFoundInx 	= true;
		int idx 				= caretIdx;
		
		// Scan each char unit \n is found and get its index :
		while(idx > 0 && notFoundInx)
		{			
			if(idx>=0 && textArea.getText().charAt(idx-1) == '\n')
			{
				// Use beginning of line :
				notFoundInx = false;
			}
			else
			{
				idx--;
			}			
		}
		
		// Insert str at index + move caret ahead :
		textArea.insertText(idx, strToInsert);
		textArea.positionCaret(caretIdx + strToInsert.length());
	}
	
	// DESCRIPTION : Inserts a string at the caret position :
	private void insertTextAtCaret()
	{
		// Declare variables :
		int caretIdx 			= textArea.getCaretPosition();
		
		textArea.insertText(caretIdx, strToInsert);
	}
	
	// DESCRIPTION : Delete a string at the start of the line :
	private void deleteTextAtStart()
	{
		// Declare variables :
		int caretIdx 			= textArea.getCaretPosition();
		boolean notFoundInx 	= true;
		int idx 				= caretIdx;	
		
		// Scan each char unit \n is found and get its index :
		while(idx > 0 && notFoundInx)
		{			
			if(idx >= 0 && textArea.getText().charAt(idx - 1) == '\n')
			{
				// Use beginning of line :
				notFoundInx = false;
			}
			else
			{
				idx--;
			}			
		}
		
		// Check if end index is not out of range :
		if((idx + strToInsert.length()) <= (textArea.getText().length()))
		{
			// Delete str at index + move caret back :
			if(textArea.getText().substring(idx, idx + strToInsert.length()).equals(strToInsert))
			{
				textArea.deleteText(idx, idx+strToInsert.length());				
				textArea.positionCaret(caretIdx - strToInsert.length());
			}
		}		
	}
	
	// DESCRIPTION : Delete a string at the caret position :
	private void deleteTextAtCaret()
	{
		// Declare variables :
		int caretIdx = textArea.getCaretPosition();
		
		if(caretIdx - strToInsert.length() >= 0)
		{
			// Delete str at index + move caret back :
			if(textArea.getText().substring(caretIdx - strToInsert.length(), caretIdx).equals(strToInsert))
			{
				textArea.deleteText(caretIdx - strToInsert.length(), caretIdx);
			}
		}	
	}
	
	// DESCRIPTION : Return key combination stats :
	public String toString()
	{
		String outStr = "[" + alt + ",\t "+ ctrl + ",\t " + shift + ",\t " + vName + "] \t " + task + " "  + insPos + " \t " + strToInsert;				
		return outStr;
	}
}
