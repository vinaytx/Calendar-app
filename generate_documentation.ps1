# Complete docx generator script
$ProgressPreference = 'SilentlyContinue'
$targetDocx = "C:\Shared\dev\Calendar-app\Group_Calendar_App_Design_Deployment_Usage.docx"
$tempDir = "$env:TEMP\calendar_docx_build"

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
New-Item -ItemType Directory -Path "$tempDir\_rels" -Force | Out-Null
New-Item -ItemType Directory -Path "$tempDir\word\_rels" -Force | Out-Null
New-Item -ItemType Directory -Path "$tempDir\docProps" -Force | Out-Null

function Save-Utf8File($filePath, $text) {
    [System.IO.File]::WriteAllText($filePath, $text, [System.Text.Encoding]::UTF8)
}

# 1. [Content_Types].xml
$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>
'@
Save-Utf8File "$tempDir\[Content_Types].xml" $contentTypes

# 2. _rels/.rels
$rootRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>
'@
Save-Utf8File "$tempDir\_rels\.rels" $rootRels

# 3. word/_rels/document.xml.rels
$docRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
'@
Save-Utf8File "$tempDir\word\_rels\document.xml.rels" $docRels

# 4. docProps/core.xml
$coreProps = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Group Calendar App: Architecture, Deployment and Usage Guide</dc:title>
  <dc:subject>Decentralized Group Calendar using Telegram Bot API</dc:subject>
  <dc:creator>Google DeepMind Advanced Agentic Coding</dc:creator>
  <cp:lastModifiedBy>Google DeepMind Advanced Agentic Coding</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">2026-11-07T05:15:00Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">2026-11-07T05:15:00Z</dcterms:modified>
</cp:coreProperties>
'@
Save-Utf8File "$tempDir\docProps\core.xml" $coreProps

# 5. docProps/app.xml
$appProps = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Word</Application>
  <DocSecurity>0</DocSecurity>
  <Company>Group Calendar Engineering Team</Company>
</Properties>
'@
Save-Utf8File "$tempDir\docProps\app.xml" $appProps

# 6. word/styles.xml
$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Calibri"/>
        <w:sz w:val="22"/>
        <w:color w:val="2D3748"/>
        <w:lang w:val="en-US"/>
      </w:rPr>
    </w:rPrDefault>
    <w:pPrDefault>
      <w:pPr>
        <w:spacing w:line="276" w:lineRule="auto" w:after="160"/>
      </w:pPr>
    </w:pPrDefault>
  </w:docDefaults>

  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
  </w:style>

  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:pPr>
      <w:jc w:val="center"/>
      <w:spacing w:before="360" w:after="120"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
      <w:b/>
      <w:sz w:val="56"/>
      <w:color w:val="1E3A8A"/>
    </w:rPr>
  </w:style>

  <w:style w:type="paragraph" w:styleId="Subtitle">
    <w:name w:val="Subtitle"/>
    <w:pPr>
      <w:jc w:val="center"/>
      <w:spacing w:before="0" w:after="360"/>
    </w:pPr>
    <w:rPr>
      <w:i/>
      <w:sz w:val="28"/>
      <w:color w:val="4B5563"/>
    </w:rPr>
  </w:style>

  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:pPr>
      <w:spacing w:before="400" w:after="160"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
      <w:b/>
      <w:sz w:val="38"/>
      <w:color w:val="1E3A8A"/>
    </w:rPr>
  </w:style>

  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:pPr>
      <w:spacing w:before="280" w:after="120"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
      <w:b/>
      <w:sz w:val="30"/>
      <w:color w:val="2563EB"/>
    </w:rPr>
  </w:style>

  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/>
    <w:pPr>
      <w:spacing w:before="200" w:after="80"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
      <w:b/>
      <w:sz w:val="24"/>
      <w:color w:val="0D9488"/>
    </w:rPr>
  </w:style>
</w:styles>
'@
Save-Utf8File "$tempDir\word\styles.xml" $styles

# BUILD DOCUMENT XML
$sb = New-Object System.Text.StringBuilder

function Escape-Xml($text) {
    if ([string]::IsNullOrEmpty($text)) { return "" }
    return $text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace('"', "&quot;").Replace("'", "&apos;")
}

function Add-P($text, $style = $null, $bold = $false, $color = $null, $size = $null) {
    [void]$sb.Append("<w:p>")
    if ($style -or $bold -or $color -or $size) {
        [void]$sb.Append("<w:pPr>")
        if ($style) { [void]$sb.Append("<w:pStyle w:val=`"$style`"/>") }
        [void]$sb.Append("<w:rPr>")
        if ($bold) { [void]$sb.Append("<w:b/>") }
        if ($color) { [void]$sb.Append("<w:color w:val=`"$color`"/>") }
        if ($size) { [void]$sb.Append("<w:sz w:val=`"$size`"/>") }
        [void]$sb.Append("</w:rPr></w:pPr>")
    }
    [void]$sb.Append("<w:r>")
    if ($bold -or $color -or $size) {
        [void]$sb.Append("<w:rPr>")
        if ($bold) { [void]$sb.Append("<w:b/>") }
        if ($color) { [void]$sb.Append("<w:color w:val=`"$color`"/>") }
        if ($size) { [void]$sb.Append("<w:sz w:val=`"$size`"/>") }
        [void]$sb.Append("</w:rPr>")
    }
    [void]$sb.Append("<w:t xml:space=`"preserve`">$(Escape-Xml $text)</w:t></w:r></w:p>")
}

function Add-Bullet($text, $boldPrefix = $null) {
    [void]$sb.Append("<w:p><w:pPr><w:ind w:left=`"720`" w:hanging=`"360`"/></w:pPr>")
    [void]$sb.Append("<w:r><w:rPr><w:color w:val=`"2563EB`"/><w:b/></w:rPr><w:t xml:space=`"preserve`">•  </w:t></w:r>")
    if ($boldPrefix) {
        [void]$sb.Append("<w:r><w:rPr><w:b/><w:color w:val=`"1E3A8A`"/></w:rPr><w:t xml:space=`"preserve`">$(Escape-Xml $boldPrefix) </w:t></w:r>")
    }
    [void]$sb.Append("<w:r><w:t xml:space=`"preserve`">$(Escape-Xml $text)</w:t></w:r></w:p>")
}

function Add-Callout($title, $body) {
    [void]$sb.Append("<w:p><w:pPr>")
    [void]$sb.Append("<w:pBdr><w:left w:val=`"single`" w:sz=`"24`" w:space=`"12`" w:color=`"2563EB`"/></w:pBdr>")
    [void]$sb.Append("<w:shd w:val=`"clear`" w:color=`"auto`" w:fill=`"F0F7FF`"/>")
    [void]$sb.Append("<w:ind w:left=`"360`" w:right=`"360`"/>")
    [void]$sb.Append("<w:spacing w:before=`"160`" w:after=`"60`"/>")
    [void]$sb.Append("</w:pPr>")
    [void]$sb.Append("<w:r><w:rPr><w:b/><w:color w:val=`"1E3A8A`"/></w:rPr><w:t xml:space=`"preserve`">$(Escape-Xml $title): </w:t></w:r>")
    [void]$sb.Append("<w:r><w:t xml:space=`"preserve`">$(Escape-Xml $body)</w:t></w:r></w:p>")
}

function Add-CodeBlock($lines) {
    [void]$sb.Append("<w:p><w:pPr>")
    [void]$sb.Append("<w:pBdr>")
    [void]$sb.Append("<w:top w:val=`"single`" w:sz=`"6`" w:space=`"4`" w:color=`"D1D5DB`"/>")
    [void]$sb.Append("<w:left w:val=`"single`" w:sz=`"6`" w:space=`"8`" w:color=`"D1D5DB`"/>")
    [void]$sb.Append("<w:bottom w:val=`"single`" w:sz=`"6`" w:space=`"4`" w:color=`"D1D5DB`"/>")
    [void]$sb.Append("<w:right w:val=`"single`" w:sz=`"6`" w:space=`"8`" w:color=`"D1D5DB`"/>")
    [void]$sb.Append("</w:pBdr>")
    [void]$sb.Append("<w:shd w:val=`"clear`" w:color=`"auto`" w:fill=`"F8FAFC`"/>")
    [void]$sb.Append("<w:ind w:left=`"240`" w:right=`"240`"/>")
    [void]$sb.Append("<w:spacing w:before=`"120`" w:after=`"120`" w:line=`"240`" w:lineRule=`"auto`"/>")
    [void]$sb.Append("</w:pPr>")
    
    $first = $true
    foreach ($line in $lines) {
        if (-not $first) { [void]$sb.Append("<w:r><w:br/></w:r>") }
        [void]$sb.Append("<w:r><w:rPr><w:rFonts w:ascii=`"Consolas`" w:hAnsi=`"Consolas`"/><w:sz w:val=`"18`"/><w:color w:val=`"1E293B`"/></w:rPr><w:t xml:space=`"preserve`">$(Escape-Xml $line)</w:t></w:r>")
        $first = $false
    }
    [void]$sb.Append("</w:p>")
}

function Add-Table($headers, $rows) {
    [void]$sb.Append("<w:tbl>")
    [void]$sb.Append("<w:tblPr>")
    [void]$sb.Append("<w:tblW w:w=`"0`" w:type=`"auto`"/>")
    [void]$sb.Append("<w:tblBorders>")
    [void]$sb.Append("<w:top w:val=`"single`" w:sz=`"8`" w:space=`"0`" w:color=`"D1D5DB`"/>")
    [void]$sb.Append("<w:left w:val=`"none`"/>")
    [void]$sb.Append("<w:bottom w:val=`"single`" w:sz=`"12`" w:space=`"0`" w:color=`"9CA3AF`"/>")
    [void]$sb.Append("<w:right w:val=`"none`"/>")
    [void]$sb.Append("<w:insideH w:val=`"single`" w:sz=`"4`" w:space=`"0`" w:color=`"E5E7EB`"/>")
    [void]$sb.Append("<w:insideV w:val=`"none`"/>")
    [void]$sb.Append("</w:tblBorders>")
    [void]$sb.Append("<w:tblCellMar><w:top w:w=`"120`" w:type=`"dxa`"/><w:bottom w:w=`"120`" w:type=`"dxa`"/><w:left w:w=`"160`" w:type=`"dxa`"/><w:right w:w=`"160`" w:type=`"dxa`"/></w:tblCellMar>")
    [void]$sb.Append("</w:tblPr>")

    # Header Row
    [void]$sb.Append("<w:tr>")
    foreach ($h in $headers) {
        [void]$sb.Append("<w:tc><w:tcPr><w:shd w:val=`"clear`" w:color=`"auto`" w:fill=`"F1F5F9`"/></w:tcPr>")
        [void]$sb.Append("<w:p><w:pPr><w:spacing w:after=`"40`"/></w:pPr><w:r><w:rPr><w:b/><w:color w:val=`"1E3A8A`"/><w:sz w:val=`"20`"/></w:rPr><w:t>$(Escape-Xml $h)</w:t></w:r></w:p></w:tc>")
    }
    [void]$sb.Append("</w:tr>")

    # Data Rows
    foreach ($row in $rows) {
        [void]$sb.Append("<w:tr>")
        foreach ($cell in $row) {
            [void]$sb.Append("<w:tc><w:p><w:pPr><w:spacing w:after=`"40`"/></w:pPr><w:r><w:rPr><w:sz w:val=`"20`"/></w:rPr><w:t>$(Escape-Xml $cell)</w:t></w:r></w:p></w:tc>")
        }
        [void]$sb.Append("</w:tr>")
    }
    [void]$sb.Append("</w:tbl><w:p><w:pPr><w:spacing w:after=`"120`"/></w:pPr></w:p>")
}

# START DOCUMENT XML
[void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
[void]$sb.Append('<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">')
[void]$sb.Append('<w:body>')

# COVER / TITLE
Add-P "Group Calendar App" "Title"
Add-P "Comprehensive Architectural Design, Production Deployment & Operations Manual" "Subtitle"
Add-P "A Serverless, Decentralized Group Availability Synchronization System Backed by Telegram Infrastructure" $null $true "2563EB" "24"
Add-P "Version: 1.0.0  |  Platform: Android 8.0+ (API 26-34)  |  Architecture: Jetpack Compose + Telegram Bot API" $null $false "6B7280" "20"
Add-P ""

Add-Callout "EXECUTIVE SUMMARY" "This document presents the full technical blueprint, security model, deployment procedures, and user operations manual for the Group Calendar App. The application eliminates the need for dedicated cloud database servers by utilizing Telegram Group Chats as a peer-to-peer (P2P) event stream and JSON object storage. Group members coordinate schedules without recurring cloud hosting fees, vendor lock-in, or database administrative burdens."

# SECTION 1
Add-P "1. System Philosophy & Decentralized Architecture" "Heading1"
Add-P "Traditional collaborative calendaring systems rely heavily on central infrastructure (e.g., PostgreSQL clusters, Firebase Realtime Database, or AWS Lambda backends). While functional, these architectures introduce recurring server costs, data privacy liabilities, authentication middleware overhead, and single points of failure."
Add-P "The Group Calendar App adopts a decentralized, peer-to-peer paradigm powered by the Telegram Bot API. By treating a Telegram group chat as an append-only event stream, every group member retains full sovereignty over their calendar data while automatically synchronizing with all peers."

Add-P "1.1 The File-Ownership & Conflict-Free Model" "Heading2"
Add-P "The foundational design rule preventing race conditions and concurrency collisions is Single-Writer File Ownership:"
Add-Bullet "Every member uniquely owns a single availability file named with their identifier: user_{userId}.json." "Deterministic Naming:"
Add-Bullet "No user can overwrite or modify another member's file. All updates are isolated to the owner's record." "Zero Write Conflicts:"
Add-Bullet "Whenever a member edits their busy times, the app compiles an updated JSON payload and posts it to the Telegram chat. Peers ingest the latest payload by comparing lastUpdated UTC timestamps." "Append-Only Event Stream:"
Add-Bullet "All downloaded availability payloads are persisted locally in the app sandbox (filesDir/member_profiles/). If offline, the app displays the last known roster state seamlessly." "Offline-First Resilience:"

# SECTION 2
Add-P "2. System Data Flow & Interaction Model" "Heading1"
Add-P "The application follows an asynchronous event-driven lifecycle across three primary phases: Broadcast, Distribution, and Aggregation."

$flowLines = @(
    "+------------------------------------------------------------------------+",
    "|                       PEER-TO-PEER DATA FLOW                           |",
    "+------------------------------------------------------------------------+",
    "  [Alice's Device]                                                        ",
    "         |                                                                ",
    "         +---> Generates user_98765.json                                  ",
    "         +---> Uploads via HTTP POST /sendDocument                        ",
    "                     |                                                    ",
    "                     v                                                    ",
    "         [Telegram Cloud Group Chat (Encrypted Event Bus)]               ",
    "                     |                                                    ",
    "         +-----------+-----------------------------------+                ",
    "         |                                               |                ",
    "         v (Polls /getUpdates)                           v (WorkManager)  ",
    "  [Bob's Device]                                  [Charlie's Device]      ",
    "         |                                               |                ",
    "         +---> Ingests user_98765.json                   +---> Ingests... ",
    "         +---> Replaces local cache                      +---> Replaces...",
    "         +---> Recalculates 'All Free' window            +---> Recalcul...",
    "         +---> Jetpack Compose UI Refreshes              +---> UI Refresh "
)
Add-CodeBlock $flowLines

Add-P "2.1 Step-by-Step Data Flow Breakdown" "Heading2"
Add-Bullet "When the user creates, modifies, or deletes a busy block, the CalendarRepository serializes the new state into a MemberAvailability object with an updated ISO-8601 UTC timestamp." "Phase 1 - Local Serialization:"
Add-Bullet "The GroupCalendarSyncManager sends a multipart/form-data POST request to Telegram's /sendDocument endpoint, attaching user_{userId}.json." "Phase 2 - Telegram Broadcast:"
Add-Bullet "Peers poll Telegram's /getUpdates API (either interactively via swipe-to-refresh or autonomously via background WorkManager). The engine filters message history for documents matching user_*.json." "Phase 3 - Log Ingestion:"
Add-Bullet "When a newer document is identified, the app queries /getFile, downloads the payload, parses the JSON, and updates the local repository cache." "Phase 4 - Reconciliation:"
Add-Bullet "The Compose AvailabilityTimeline calculates intersecting free intervals and re-renders the color-coded roster track." "Phase 5 - UI Presentation:"

# SECTION 3
Add-P "3. Data Structures & Serialization Schema" "Heading1"
Add-P "All calendar data adheres strictly to the schema below. This ensures interoperability across all client devices and future platforms (iOS, desktop, or web)."

Add-P "3.1 MemberAvailability.json Schema" "Heading2"
$jsonSchema = @(
    "{",
    '  "userId": "user_98765",',
    '  "userName": "Alice Smith",',
    '  "lastUpdated": "2026-11-07T05:30:00Z",',
    '  "colorHex": "#3B82F6",',
    '  "busySlots": [',
    '    {',
    '      "start": "2026-11-08T09:00:00",',
    '      "end": "2026-11-08T11:00:00",',
    '      "title": "Strategy Sync"',
    '    },',
    '    {',
    '      "start": "2026-11-08T14:30:00",',
    '      "end": "2026-11-08T16:00:00",',
    '      "title": "Client Review"',
    '    }',
    '  ]',
    "}"
)
Add-CodeBlock $jsonSchema

Add-P "3.2 Schema Field Definitions" "Heading2"
$headers = @("Field Name", "Type", "Required", "Description")
$rows = @(
    @("userId", "String", "Yes", "Globally unique identifier generated during client initialization (e.g., user_a1b2c3d4)."),
    @("userName", "String", "Yes", "Human-readable display name shown on group availability tracks."),
    @("lastUpdated", "String (ISO-8601)", "Yes", "UTC timestamp used for chronological reconciliation and cache invalidation."),
    @("colorHex", "String (Hex)", "No", "Hexadecimal accent color (e.g. #3B82F6) for personalizing the timeline avatar."),
    @("busySlots", "Array of Objects", "Yes", "List of occupied intervals. Any hour outside these slots is marked Free."),
    @("busySlots.start", "String (ISO-8601)", "Yes", "Start timestamp of the busy interval (e.g. 2026-11-08T09:00:00)."),
    @("busySlots.end", "String (ISO-8601)", "Yes", "End timestamp of the busy interval (e.g. 2026-11-08T11:00:00)."),
    @("busySlots.title", "String", "No", "Optional description/tag displayed when a busy block is tapped.")
)
Add-Table $headers $rows

# SECTION 4
Add-P "4. Android Technical Implementation" "Heading1"
Add-P "The application is built in Kotlin targeting Android 14 (API 34) with backward compatibility to Android 8.0 (API 26). It employs Google's recommended modern Android architecture:"

Add-P "4.1 Key Architecture Components" "Heading2"
Add-Bullet "Built entirely using Material 3 and Jetpack Compose. Replaces fragile XML layouts with reactive, declarative UI components." "Jetpack Compose UI:"
Add-Bullet "Custom Gantt-style timeline component that dynamically measures time boundaries, parses busy intervals, and maps them to horizontal pixel coordinates." "AvailabilityTimeline Component:"
Add-Bullet "Acts as the single source of truth for the UI layer, exposing StateFlow<Map<String, MemberAvailability>>." "CalendarRepository:"
Add-Bullet "Executes PeriodicWorkRequest with network constraints (NetworkType.CONNECTED) every 15 minutes to guarantee background updates." "WorkManager Background Sync:"
Add-Bullet "Type-safe HTTP client with OkHttp connection pooling, automatic retries, and Gson deserialization." "Retrofit 2 & OkHttp 4:"

# SECTION 5
Add-P "5. Security Isolation & Credential Architecture" "Heading1"
Add-P "A major vulnerability in naive bot implementations is hardcoding bot tokens and chat IDs directly into version control or decompilable Java bytecode. The Group Calendar App implements a three-tier security isolation model:"

Add-Bullet "Developer keys are placed exclusively in local.properties. Gradle injects them into BuildConfig at compile time. local.properties is ignored by Git by default." "Tier 1 - Compile-Time Isolation (local.properties):"
Add-Bullet "If the app is built without pre-compiled keys, the Settings and Onboarding screens allow users to input or update their own Bot Token and Group Chat ID dynamically. Values are stored in private SharedPreferences." "Tier 2 - Runtime Dynamic Override:"
Add-Bullet "Multiple independent friend groups, families, or corporate teams can use the identical APK binary. Each group merely points their app to their own private Telegram Bot and Group Chat ID." "Tier 3 - Multi-Tenant Segregation:"

# SECTION 6
Add-P "6. APK Build, Cryptographic Signing & Sideload Distribution" "Heading1"
Add-P "Distributing apps directly via APK files (sideloading) provides complete freedom from Google Play Store review queues, policy changes, and developer account fees. However, proper cryptographic signing is required for Android OS package validation."

Add-P "6.1 Generating a Production Release Keystore" "Heading2"
Add-P "Run the following command using the JDK keytool to generate an RSA 2048-bit release keystore with 25-year validity:"
$keystoreCmd = @(
    "keytool -genkey -v -keystore my-release-key.jks \",
    "        -keyalg RSA -keysize 2048 -validity 10000 \",
    "        -alias my-app-key"
)
Add-CodeBlock $keystoreCmd

Add-P "6.2 Configuring Gradle Release Signing" "Heading2"
Add-P "Store the passwords in local.properties:"
$localProps = @(
    "RELEASE_KEYSTORE_PATH=my-release-key.jks",
    "RELEASE_KEYSTORE_PASSWORD=your_secure_keystore_password",
    "RELEASE_KEY_ALIAS=my-app-key",
    "RELEASE_KEY_PASSWORD=your_secure_key_password"
)
Add-CodeBlock $localProps

Add-P "6.3 Executing the Build" "Heading2"
Add-P "To produce the signed, optimized release APK, execute:"
Add-CodeBlock @("./gradlew assembleRelease")
Add-P "The resulting file will be located at: app/build/outputs/apk/release/app-release.apk"

Add-P "6.4 Distribution Channels" "Heading2"
Add-Bullet "Directly drag and drop the app-release.apk file into the Telegram group chat! Group members can tap the file directly in Telegram to install it." "Direct Telegram Distribution:"
Add-Bullet "Push tags (e.g. v1.0.0) to GitHub and attach app-release.apk as a binary asset. Provides a permanent download link." "GitHub Releases:"
Add-Bullet "Users can install Obtainium (open source) on Android and enter the GitHub repo URL. Obtainium will automatically notify users and update the APK whenever a new release tag is pushed." "Automated Package Trackers (Obtainium):"

# SECTION 7
Add-P "7. Overcoming Android OS Sideloading Barriers" "Heading1"
Add-P "Because direct APK installation bypasses Google Play, modern versions of Android (Android 8 through 14+) display safety prompts. Here is the exact guidance to provide end users:"

Add-P "Barrier 1: 'Install Unknown Apps' Permission" "Heading2"
Add-P "Android disables sideloading by default per-application to prevent malware. When the user taps the APK in Telegram, Chrome, or File Manager:"
Add-Bullet "Tap 'Settings' on the popup dialog." "Step 1:"
Add-Bullet "Toggle 'Allow from this source' to ON." "Step 2:"
Add-Bullet "Press the Back button and tap 'Install'." "Step 3:"

Add-P "Barrier 2: Google Play Protect Warning" "Heading2"
Add-P "Google Play Protect scans APKs that are not indexed in the Play Store catalog and may show: 'Blocked by Play Protect' or 'Unrecognized app'."
Add-Bullet "Tap 'More details' (small expand arrow)." "Step 1:"
Add-Bullet "Tap 'Install anyway'." "Step 2:"
Add-Callout "NOTE FOR USERS" "This warning is completely normal for private and self-built open-source applications. The app runs in standard Android application sandboxing with zero elevated root privileges."

# SECTION 8
Add-P "8. In-App Self-Updating Engine" "Heading1"
Add-P "To eliminate manual re-downloading for future updates, the application includes a self-updating system built into ApkUpdateHelper.kt:"

Add-Bullet "The app checks a designated URL (or GitHub latest release asset) for an updated versionCode." "1. Remote Version Check:"
Add-Bullet "OkHttpClient streams the binary to context.cacheDir/update_app.apk with real-time download progress tracking (0% to 100%)." "2. Progress Streaming:"
Add-Bullet "A secure FileProvider generates a content:// URI granting temporary read permission to Android's PackageInstaller." "3. FileProvider URI Resolution:"
Add-Bullet "The app fires an ACTION_VIEW intent with MIME type application/vnd.android.package-archive. Android's native system installer handles the seamless in-place upgrade without wiping user data." "4. Package Installer Invocation:"

# SECTION 9
Add-P "9. Complete Deployment & User Operations Guide" "Heading1"
Add-P "Follow this step-by-step walkthrough to set up the Telegram infrastructure and deploy the calendar across a team or group."

Add-P "Step 1: Create the Telegram Bot" "Heading2"
Add-Bullet "Open Telegram and search for @BotFather (verified bot with blue checkmark)." "Action 1:"
Add-Bullet "Send /newbot and provide a friendly name (e.g. 'Team Calendar Bot') and unique username ending in 'bot' (e.g. 'AlphaTeamCalendar_bot')." "Action 2:"
Add-Bullet "BotFather will output your HTTP API Token: e.g. 123456789:ABCdefGHIjklMNOpqrSTUvwxYZ. Copy this token." "Action 3:"
Add-Bullet "IMPORTANT: Send /setprivacy to @BotFather, select your bot, and set it to 'DISABLED'. This ensures the bot can read JSON document uploads in group chats." "Action 4:"

Add-P "Step 2: Create the Telegram Group & Add the Bot" "Heading2"
Add-Bullet "Create a new Telegram Group (e.g. 'Alpha Team Roster')." "Action 1:"
Add-Bullet "Add your newly created bot to the group as an Administrator with 'Send Messages' and 'Send Files' permissions." "Action 2:"
Add-Bullet "Generate a Group Invite Link (Group Settings -> Invite Links -> Create New Link) to share with members." "Action 3:"

Add-P "Step 3: Determine the Group Chat ID" "Heading2"
Add-Bullet "In your browser, visit: https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates" "Action 1:"
Add-Bullet "Send a test message (e.g., 'hello') inside the Telegram group chat." "Action 2:"
Add-Bullet "Refresh the browser page. Look for the 'chat' JSON object: 'chat':{'id': -1001234567890, 'title': 'Alpha Team Roster'}. The negative number is your Group Chat ID." "Action 3:"

Add-P "Step 4: Distribute App & Onboard Users" "Heading2"
Add-Bullet "Share the compiled app-release.apk and the Telegram Group Invite Link with your members." "Action 1:"
Add-Bullet "Members install the APK and launch the app." "Action 2:"
Add-Bullet "The 3-step Onboarding Wizard guides them to enter their name, choose an accent color, and connect to the group." "Action 3:"
Add-Bullet "Members tap 'Add Busy Slot' to configure their occupied hours. The app instantly synchronizes the entire roster!" "Action 4:"

# SECTION 10
Add-P "10. Troubleshooting & FAQ" "Heading1"

$faqHeaders = @("Symptom / Error", "Root Cause", "Resolution")
$faqRows = @(
    @("HTTP 403 Forbidden on upload", "Bot is not a member of the group or lacks permissions", "Add the bot to the Telegram group and grant it Administrator rights with document posting permissions."),
    @("getUpdates returns empty result", "Telegram Privacy Mode is blocking group messages", "Send /setprivacy to @BotFather, select your bot, and choose 'DISABLED'."),
    @("Sync works on WiFi but not in background", "Android OS battery optimization restricting background work", "In Android Settings -> Apps -> Group Calendar -> Battery, change setting to 'Unrestricted'."),
    @("Play Protect blocks APK install", "Signature is not indexed in Google Play Store database", "Instruct users to tap 'More details' and then 'Install anyway'."),
    @("App fails to install over previous build", "Conflicting debug vs release signing keystore", "Uninstall the debug version first before installing a release-signed APK.")
)
Add-Table $faqHeaders $faqRows

Add-P ""
Add-P "Document Concluded. For source code repository and updates, inspect C:\Shared\dev\Calendar-app." $null $true "4B5563" "18"

[void]$sb.Append('</w:body></w:document>')

Save-Utf8File "$tempDir\word\document.xml" $sb.ToString()

# PACKAGE INTO ZIP AND RENAME TO DOCX
$tempZip = "$env:TEMP\calendar_temp.zip"
if (Test-Path $tempZip) { Remove-Item $tempZip -Force }
if (Test-Path $targetDocx) { Remove-Item $targetDocx -Force }

Compress-Archive -Path "$tempDir\*" -DestinationPath $tempZip -Force
Move-Item -Path $tempZip -Destination $targetDocx -Force

if (Test-Path $targetDocx) {
    $fileItem = Get-Item $targetDocx
    Write-Host "SUCCESS: Word document successfully created at: $targetDocx"
    Write-Host "File size: $([math]::Round($fileItem.Length / 1KB, 2)) KB"
} else {
    Write-Error "Failed to generate Word document."
}
