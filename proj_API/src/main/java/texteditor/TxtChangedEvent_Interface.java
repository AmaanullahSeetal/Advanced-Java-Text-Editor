package texteditor;

public interface TxtChangedEvent_Interface
{
	TxtChangedCallbk_Interface getCallbkEvent();
	String getTextToDetect();
}
