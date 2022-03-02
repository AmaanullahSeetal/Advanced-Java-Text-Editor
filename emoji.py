import texteditor.TxtChangedCallbk_Interface
import texteditor.TxtChangedEvent_Interface

class emojiHandler(texteditor.TxtChangedCallbk_Interface):

	def notifyTxtChangeHappened(self, event):
		
		# Get the text detected by event :
		txtDetected = event.getTextToDetect()
		
		# Get caret position :
		caretPos = api.getCaretPosition()
		
		# Get text from editor :
		txt = api.getText()	
		
		# Get emoji unicode char :
		emoji = u"\U0001f60a"
		
		# Replace text with emoji :
		str = txt.replace(txtDetected, emoji)		
		
		# Replace all text in editor :
		api.setText(str)
		
		# Place caret back :
		api.setCaretPosition(caretPos)
		

api.addTextChangeCallbk(":-)", emojiHandler())
api.displayInListView("Smiley Emoji Script")