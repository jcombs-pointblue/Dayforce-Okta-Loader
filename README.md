# DayforceOktaLoader

Loads users from a CSV file into Okta. Each row is matched against the
existing Okta population on a configurable join key; matching rows are
patched and unmatched rows are created.

## Requirements

- Java 8 or later
- Maven 3.x
- Network access to your Okta tenant

## Build

```sh
mvn clean package
```

Produces a runnable shaded jar at `target/DayforceOktaLoader.jar`.

## Run

The application reads two property files **from the current working
directory**:

- `csvSync.properties` — runtime configuration
- `attributeMap.properties` — CSV-column → Okta-attribute mapping

`csvSync.properties` is gitignored because it holds an encrypted token
and local paths. On a fresh clone, copy the template and edit it:

```sh
cp csvSync.properties.template csvSync.properties
$EDITOR csvSync.properties
```

Then run:

```sh
java -jar target/DayforceOktaLoader.jar
```

Logs are written to the directory configured by `logPath` with a
timestamped filename (`YYYYMMDD-HHmmss-csvLoader.log`).

## Creating the Okta API token

The loader authenticates with an Okta **SSWS API token** (sent as
`Authorization: SSWS <token>`).

1. Sign in to the Okta admin console (`https://<tenant>-admin.okta.com`)
   as a Super Admin or an admin with API token rights.
2. **Security → API** (left nav).
3. **Tokens** tab → **Create token**.
4. Give it a descriptive name (e.g. `dayforce-loader-prod`) and click
   **Create token**.
5. **Copy the token immediately** — Okta only displays it once.

Notes:

- The token inherits the permissions of the admin who created it. This
  loader needs at least **User Admin** rights (read users, create and
  modify profile fields). Super Admin works but is more privilege than
  required.
- Tokens expire after **30 days of inactivity**. Any successful call
  resets the counter, so a regularly scheduled job effectively keeps the
  token alive.
- The token is tied to the admin's account — if the admin is
  deactivated, the token dies with it. Use a dedicated service account
  if possible.
- If your tenant uses API Access Management with network zones, restrict
  the token to your job runner's egress IPs.

## Encrypting the API token

The `token` value in `csvSync.properties` must be the encrypted form
produced by `CredentialUtility`. To encrypt a fresh Okta SSWS token:

```sh
java -cp target/DayforceOktaLoader.jar \
  com.pointbluetech.okta.csv.CredentialUtility 'paste-the-plain-token-here'
```

Copy the resulting Base64 string into the `token =` line of
`csvSync.properties`.

> The encryption secret is hardcoded in `CredentialUtility.java`
> (`secretString`). Change it per deployment so encrypted tokens from one
> deployment cannot be read by another. Encrypted tokens move cleanly
> between Linux and Windows.

## Checking the connection

`OktaConnection` validates the `oktaURL` + `token` pair and lists every
user the token can see — useful as a smoke test before running a full
load. Reads `csvSync.properties` from the current working directory.

```sh
java -cp target/DayforceOktaLoader.jar \
  com.pointbluetech.okta.csv.OktaConnection
```

Prints `Connection OK. Okta org id: <id>` on success, followed by the
full pretty-printed JSON for each user. Throws on a bad URL, rejected
token, or unreachable tenant. Also writes to
`<logPath>okta-connection-check.log`.

## `csvSync.properties`

| Key | Required | Default | Description |
|---|---|---|---|
| `oktaURL` | yes | — | Tenant base URL, e.g. `https://example.okta.com` (no trailing slash). |
| `token` | yes | — | Encrypted SSWS token. See above. |
| `inputFilePath` | yes | — | Absolute path to the CSV file. |
| `logPath` | yes | — | Directory where log files are written. Must exist; trailing `/` required. |
| `threads` | yes | — | Number of worker threads reading the CSV in parallel. Each thread processes every Nth row. |
| `loginSuffix` | yes | `""` | String appended to the match-key value to form the Okta `login`, e.g. `@example.edu`. |
| `matchKey` | no | `universalID` | CSV column **and** Okta profile attribute used to join CSV rows to existing Okta users. Must exist on both sides. Not declared in `attributeMap.properties`. |
| `dateField.<csv>.fromFormat` | no | — | `SimpleDateFormat` pattern for parsing the inbound CSV value. |
| `dateField.<csv>.toFormat` | no | — | `SimpleDateFormat` pattern for the value sent to Okta. Both `from` and `to` must be set for a transform to apply. Empty values pass through; unparseable values are logged and pass through unchanged. |
| `proxyHost` | no | — | Optional outbound HTTP proxy hostname. |
| `proxyPort` | no | `0` | Required if `proxyHost` is set. |

### Example

```properties
oktaURL = https://example.okta.com
token = base64-encrypted-blob...
inputFilePath = /opt/loads/students.csv
logPath = /var/log/okta-loader/
threads = 4
loginSuffix = @example.edu

matchKey = universalID

dateField.graduationDate.fromFormat = yyyy-MM-dd
dateField.graduationDate.toFormat = MM-dd-yyyy
```

## `attributeMap.properties`

Maps **CSV column name** → **Okta profile attribute name**, one per line.
Used for both create and modify operations.

```properties
# CSV header        Okta attribute
email             = email
firstName         = firstName
lastName          = lastName
homeEmail         = secondEmail
middleName        = initials
academicLevel     = academicLevel
classStanding     = classStanding
affiliation       = eduPersonAffiliation
graduationDate    = graduationDate
```

Notes:

- **Do not** include `matchKey` (default `universalID`) in this file. It is
  handled specially: copied verbatim from CSV into the Okta profile and
  also used to join.
- The CSV `login` value is computed from `matchKey + loginSuffix` and is
  not configurable through this file.
- Either `=`, `:`, or whitespace separates the key and value
  (`Properties.load` semantics).
- Columns present in the CSV but absent from this file are ignored.
- Columns listed here but missing from the CSV are simply unmapped — no
  error.

## CSV format

- UTF-8 encoded. A leading byte-order mark is tolerated and stripped.
- First row is a header. Header names are trimmed of surrounding whitespace.
- Quoted values with embedded commas are supported (commons-csv default
  format).
- A column matching the configured `matchKey` is required.
- Empty cells are sent to Okta as empty strings.

Minimal example (default `matchKey = universalID`):

```csv
universalID,firstName,middleName,lastName,email,homeEmail,academicLevel,classStanding,affiliation,graduationDate
10000001,Pat,Q,Smith,pat.smith@example.edu,pat@home.example,UG,SR,STUDENT,2026-05-15
```

## How matching works

1. On startup the loader pages through `/api/v1/users` and indexes every
   active Okta user by `profile.<matchKey>`. Users without that attribute
   set are skipped.
2. Worker threads stream the CSV. For each row, `recordMap[matchKey]` is
   looked up in the index.
   - **Hit:** every CSV column with an entry in `attributeMap.properties`
     is compared to the current Okta value. Differences are sent as a
     `POST /api/v1/users/{id}` partial profile update.
   - **Miss:** a `POST /api/v1/users?activate=false` create request is
     issued with all mapped fields plus `login = matchValue + loginSuffix`
     and the `matchKey` itself.

Created users are **not activated** automatically — wire activation into
your Okta lifecycle policy or run a follow-up step.

## Threading

`threads = N` spawns N workers. Worker `i` (1..N) processes rows where
`lineNumber % N == i`, so order across threads is interleaved. Increase
threads for throughput; respect Okta's per-tenant rate limits (the loader
does not currently throttle adaptively).

## Troubleshooting

- **`Error while decrypting:` on startup** — the `token` value is corrupt
  or was encrypted with a different `secretString`. Re-run
  `CredentialUtility` and replace the value.
- **`User creation failed`** — check the log for the Okta error body. The
  most common causes are required attributes the CSV doesn't supply, a
  duplicated `login` on a deactivated user, or schema mismatch between an
  Okta attribute name in `attributeMap.properties` and the actual profile
  schema.
- **`Skipping row N: missing <matchKey>`** — the CSV row has a blank
  match-key value. Fix upstream.
- **`Date parse failed for graduationDate=...`** — the value didn't match
  `dateField.graduationDate.fromFormat`. The original string is sent
  through unchanged; correct the CSV or widen the format.
- **Wrong attribute set on a freshly created user but a different one
  updated on subsequent runs** — historical bug; verify
  `attributeMap.properties` is the only source of mappings (no inline
  hardcoded mappings remain in the create path).
