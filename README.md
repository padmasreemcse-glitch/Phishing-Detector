# Phishing URL Scanner

A Java web app that checks a URL for common phishing warning signs, gives it a
risk score out of 100, and saves every scan to MySQL.

- **Frontend:** plain HTML, CSS and JavaScript (`frontend/`)
- **Backend:** Java servlets on Tomcat, built with Maven (`backend/`)
- **Database:** MySQL (`db/`)

## How scoring works

Each rule adds 20 points. A score of 40 or more is reported as **Suspicious**.

| Rule | Points |
|---|---|
| URL does not use `https` | 20 |
| URL contains `@` | 20 |
| URL contains `login`, `verify` or `password` | 20 |
| URL is longer than 100 characters | 20 |
| Host is a raw IP address | 20 |

This is a simple rule-based check. A low score does not prove a site is safe.

## Project layout

```
backend/
  ScanServlet.java     POST /scan      analyses a URL and saves the result
  HistoryServlet.java  GET  /history   returns the latest scans as JSON
  UrlAnalyzer.java     scoring rules
  ScanDao.java         saves a scan to MySQL
  DBUtil.java          opens MySQL connections (used by HistoryServlet)
  ScanResult.java      one saved scan row
  DriverLoader.java    loads the MySQL driver when the app starts
  pom.xml              Maven build file (produces phishing-detector.war)
frontend/
  index.html, style.css, app.js
db/
  schema.sql           creates the database and the scans table
  queries.sql          history and analytics queries (optional)
  phishingdb_scans.sql a sample MySQL dump (optional)
```

## What you need installed

| Tool | Version | Check with |
|---|---|---|
| JDK | 17 or newer | `java -version` |
| Maven | 3.9 or newer | `mvn -version` |
| Apache Tomcat | **10.1 or newer** (Tomcat 9 will not work) | |
| MySQL | 8.x or 9.x | `mysql --version` |

The code uses `jakarta.servlet`, which only Tomcat 10.1+ supports.

### Installing the tools on Windows (PowerShell)

```powershell
# JDK 17
winget install EclipseAdoptium.Temurin.17.JDK

# Maven (not in winget, so download it)
Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" -OutFile "$HOME\Downloads\maven.zip"
Expand-Archive "$HOME\Downloads\maven.zip" -DestinationPath C:\

# Tomcat 10.1
Invoke-WebRequest "https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.34/bin/apache-tomcat-10.1.34.zip" -OutFile "$HOME\Downloads\tomcat.zip"
Expand-Archive "$HOME\Downloads\tomcat.zip" -DestinationPath C:\
```

MySQL: install MySQL Community Server and MySQL Workbench from mysql.com.

**Close PowerShell and open a new window after installing.** Windows only reads
PATH when a window starts.

If `java`, `mvn` or Tomcat later complain about `JAVA_HOME` or a missing
command, run this in the same window (it only lasts for that window):

```powershell
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:Path += ";$($jdk.FullName)\bin;C:\apache-maven-3.9.9\bin"
```

To set `JAVA_HOME` permanently:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", $jdk.FullName, "User")
```

## Setup and run

### 1. Create the database

Make sure MySQL is running (`Get-Service MySQL*` should say `Running`).

The easiest way is MySQL Workbench: open `db/schema.sql` and run it.

From the command line, log in and use `source` (the `<` redirect does not work
in PowerShell):

```
mysql -u root -p
source C:/path/to/phishing-detector/db/schema.sql
exit
```

Use forward slashes in the path. If your root user has no password, run
`mysql -u root` without `-p`.

Check it worked: `SHOW DATABASES;` should list `phishingdb`.

### 2. Set your MySQL password (two files)

Open **both** files in `backend/` and change the password line:

- `ScanDao.java`  - `PASSWORD = "YOUR_PASSWORD"`
- `DBUtil.java`   - `PASSWORD = "YOUR_PASSWORD"`

Use your own MySQL root password, or `""` if root has no password. The username
is `root` and the database is `phishingdb` on `localhost:3306`.

Do not commit your real password to a shared repository.

If you edit the files in PowerShell rather than an editor, do not use
`Set-Content -Encoding UTF8`. It adds a hidden marker that breaks Java
compilation. Edit in an editor, or use `[IO.File]::WriteAllText` with
`UTF8Encoding($false)`.

### 3. Build the WAR

```powershell
cd backend
mvn package
```

The first build downloads libraries and takes a few minutes. You want
`BUILD SUCCESS` and the file `backend/target/phishing-detector.war`.

The password is compiled into the WAR, so rebuild after any change to it.

### 4. Deploy to Tomcat

Replace the path with your Tomcat folder:

```powershell
Remove-Item -Recurse -Force C:\apache-tomcat-10.1.34\webapps\phishing-detector -ErrorAction SilentlyContinue
Copy-Item target\phishing-detector.war C:\apache-tomcat-10.1.34\webapps\ -Force
```

### 5. Start Tomcat

```powershell
cd C:\apache-tomcat-10.1.34\bin
.\catalina.bat run
```

`JAVA_HOME` must be set in this window (see above). Wait for a line like
`Server startup in [...] milliseconds`, and look for `MySQL JDBC driver loaded`.
Leave this window open. Press **Ctrl+C** to stop Tomcat.

### 6. Open the app

**http://localhost:8080/phishing-detector/**

### 7. Test it

- `https://example.com` should score **0** (No major warning detected).
- `http://192.168.1.5/login` should be **Suspicious** (score 60).
- Both scans should appear in the Recent Scans table.

## Redeploying after a code change

Stop Tomcat (Ctrl+C), then repeat steps 3, 4 and 5.

## Troubleshooting

Check the Tomcat window (or `logs\*.log` in the Tomcat folder) for the error.

| Symptom or error | Cause and fix |
|---|---|
| Page shows "Database operation failed" | See the specific SQL error below |
| `Access denied for user 'root'@'localhost'` | Wrong password in `ScanDao.java` / `DBUtil.java`. Fix both, rebuild, redeploy |
| `Unknown database 'phishingdb'` or `Table ... doesn't exist` | `db/schema.sql` has not been run |
| `Communications link failure` | MySQL is not running, or not on port 3306 |
| `No suitable driver found` | An old WAR is deployed. Delete `webapps\phishing-detector`, copy the new WAR, restart |
| 404 on the page | Wrong address or the WAR did not deploy. The path is the WAR name: `/phishing-detector/` |
| `java` / `mvn` is not recognised | Not installed, or PATH not loaded. Open a new window or use the PATH snippet above |
| `JAVA_HOME ... not defined correctly` | Set `JAVA_HOME` (see above) |
| `illegal character: '\ufeff'` when building | A Java file was saved with a BOM. Re-save it as UTF-8 without BOM |
| Port 8080 already in use | Another Tomcat or program is using it. Stop it, or change the port in Tomcat's `conf\server.xml` |

## Notes

- `ScanDao.java`, `ScanServlet.java` and `UrlAnalyzer.java` are the original
  core files and are kept as written.
- `DBUtil.java` and `ScanDao.java` each hold their own copy of the connection
  settings, so keep them in sync.
