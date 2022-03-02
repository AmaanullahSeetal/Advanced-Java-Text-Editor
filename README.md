# Advanced Java Text Editor

## Description
- This is a GUI-based Java text editor
- It has an in-built API for the addition of plugins to extend its base functionalities
- It also has a built-in DSL parser to map shortcut keys from a keymap file

<div align="center">
    <img src="https://i.imgur.com/RFJayeN.png">
    <span style="display:table-cell;height:20px;"></span>
</div>

<br/>

## How to Run:

1. To run the application with the default locale (en-AU : english), use a terminal and execute:

````
	./gradlew run
````

2. To run the application with the alternative locale (fr-FR : french), use a terminal and execute:

````
	./gradlew run --args='--locale=fr-FR'
````

<br/>


## Sample Java plugins:

### Date Plugin : 
- Location : `\proj_pluginDate\bin\src\main\java\texteditor\DatePlugIn.class`
- Details : It adds a new button labelled “Date” to the taskbar. It inserts the current date and time when pressed.

### Find Plugin : 
- Location : `\proj_pluginFind\bin\src\main\java\texteditor\FindPlugIn.class`
- Details : It adds a new button labelled “Find” to the taskbar. When pressed, a prompt is displayed requesting the user for a search term. Finally it highlights the word if found.

<br/>


## Sample Python plugin:

### Emoji Plugin : 
- Location : `\emoji.py`
- Details : It automatically converts ":-)" to 😊 emojis in the text
<br/><br/>

## Connect with me:
[<img align="left" alt="LinkedIn.com" width="22px" src="https://i.imgur.com/FDQIUtd.jpg" style="padding-right:10px;"/>][website]

<br/>

[website]: https://www.linkedin.com/in/amaan-seetal/