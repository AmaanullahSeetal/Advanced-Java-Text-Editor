package texteditor;

import java.util.Locale;

public interface API 
{	
	void addButton(String buttonName, BtnPressCallbk_Interface callbkEvent);	
	void highlightText(int startIndex, int endIndex);		
	String showDialog();	
	int getCaretPosition();
	void setCaretPosition(int caretPosition);
	String getText();
	void setText(String text);
	Locale getLocale();
	void displayInListView(String displayName);	
	void addKeyPressCallbk(String keyName, KeyPressCallbk_Interface eventKeyPress);
	void addTextChangeCallbk(String txtDetected, TxtChangedCallbk_Interface eventCallbk);
}
