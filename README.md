This file was created by IntelliJ IDEA 10.5.2 for binding GitHub repository















Decoding the Notes Calendar fields.

Use the following URL which will give you the design of the view, which includes
names for all fields in the calendar view.
https://company.com/mail/user.nsf/($Calendar)?ReadDesign

See calendar-design-sample.xml for what was returned in my case, and therefore
what this code assumes each field means.

To find the proper icloud URL.. go in to ~/Library/Calendars and walk through all
the directories. Open each Info.plist file. Look for the one that has a key
of Title with its value the name of your calendar. Then grab the Calendar Path
above.

Mac Jar Bundler is in /usr/share/java/Tools
