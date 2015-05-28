# Datanose
<img src="https://github.com/RickHutten/DataNose/blob/master/Doc/device-2015-01-09-115922.png" width="200" /><img src="https://github.com/RickHutten/DataNose/blob/master/Doc/device-2015-01-29-153153.png" width="200" /><img src="https://github.com/RickHutten/DataNose/blob/master/Doc/device-2015-01-29-134157.png" width="200" />

### App
The app lets FNWI students quickly acces their schedule. The user has to enter his or her student ID and the app fetches the schedule from https://datanose.nl/. There is also an option avaiable to sync the schedule to the users local calendar.

### Known bugs
There is a bug when the user syncs his/her schedule, signs out, logs in with a different student ID and syncs that other schedule i.e. syncs to quickly and doesn't wait for the first synchronization to complete. Is it possible that the synchronization is not perfectly completed. When the calendar is corrupted, the user has to manually delete the calendar from his default calendar app. It is recommended to wait for the synchronization to complete before starting another sync operation.

### Credits

Icons downloaded from https://developer.android.com/design/downloads/index.html.

Some elements including buttons, checkboxes, colorpicker and circular progress bar belong to the MaterialDesign library (https://github.com/navasmdc/MaterialDesignLibrary).

The date picker belongs to the Betterpickers library (https://github.com/derekbrameyer/android-betterpickers).

---

### Copyright<img src="https://github.com/RickHutten/DataNose/blob/master/app/src/main/res/drawable/datanose_logo_small.png" align="right" width="100" />
DataNose &copy; by Rick Hutten.

DataNose is licensed under a
Creative Commons Attribution-ShareAlike 3.0 Unported License.

You should have received a copy of the license along with this
work.  If not, see http://creativecommons.org/licenses/by-sa/3.0/.

