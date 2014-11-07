featherdb
=========

FeatherDB - Java based document database

This is a code dump from 2008. Here is an archived copy of the blog post that went along with is as some sort of documentation.

--------

I've been holding onto this code for a while now (months)...  I haven't finished polishing is, and chances are I won't.  But there are a few important concepts in here that I think are important to get out into the open.

FeatherDB was my attempt at a Java clone of CouchDB.  When I first found out about CouchDB, I was very intrigued.  A non-relational database fits one of my projects perfectly... when you find yourself fighting with Hibernate just to get a schema mapped, there is an issue on one side or the other.  In this case it was me. [I should note that I actually like relational databases for most things, it just so happens that my particular application doesn't map well to a static schema]

So, I did what any self-respecting geek would do - I downloaded the code and tried to get it running.  And that led to the first problem: I was on a Windows machine and the (at the time) binary download for Windows washorribly out of date.  Not to fear though, I had a linux box handy, so I just moved over to that machine.  I downloaded CouchDB, and got it running.  Then I had the problem that the java library wasn't compatible with the newer JSON syntax (the old version was all XML, all the time).
  No problem there either... I spend a day or two getting my feet wet with the REST API, and wrote a new library couchdb4j.

Now, I was off and running working with CouchDB, and non-relational nirvana. 

Or at least I should have been.  At the time, CouchDB was (and is) very much a work in progress... there were a bunch of things that were planned but were missing, such as: support for stored/named views and any sort of authentication.  No problem!  I'll just add this myself!

Problem: CouchDB is written in Erlang.

I don't do Erlang.

I don't have anything against Erlang... we've never met.  I just don't want to learn another language.  Especially one that is so specialized.  I know that I should learn a language a year, but Erlang was just too much of a hurdle for me to get in and start mucking around in code.  Plus, would you really accept a patch from a guy that is just learning a language?  Neither would I.

No problem!  I'll write my own Java version of CouchDB... and do it better.  The result was/is FeatherDB. 

FeatherDB is a Java based document database.  It has an all HTTP/REST interface.  It supports querying by a Java class (added as a jar to the server), or via JavaScript (uses Java6's JavaScript support).  In theory you could also search by any language that is on the JVM and implements the correct interface.

FeatherDB allows for flexibility in the backend storage, allowing you to use any backend you'd like.  I wrote three: a filesystem based backend, an all-in-memory backend, and a caching backend that uses an in-memory hash and punts to a supporting backend when needed.  (I'd like to add a BerkeleyDB or Derby backend to make things a bit more resilient).

FeatherDB uses an embedded Jetty HTTP server to handle all interaction. 

There were a few things about the way CouchDB handles documents that I didn't like, so FeatherDB does things a little bit differently. 

Like CouchDB:

1. Documents are accessed through the HTTP interface by a REST API.
1. Documents are versioned.  Always.  You can access any version of any document.
1. Document information is stored and retrieved in JSON format.
1. The Document space is "flat".  Documents can link to other documents, but there is no concept of "joining" documents.
1. You query the database through 'views'.  Views are also documents.

Unlike CouchDB (last I checked):

1. Access is controlled through an authentication layer.
  * Users are authenticated
    * though HTTP-Auth or 
    * by loading /_auth?username=foo&password=bar or 
    * via a previously generated 'token' (an authenticated user can generate a token, and pass it to an unauthenticated user for temporary access)
  * Users can be restricted to read-only, write-only, or read-write access to a database (document-level ACLs proved to be too slow).

1. Documents can have "common" data that is shared among all revisions.
  * Any attribute whose name starts with "_" is determined to be "common".
  * When you update that attribute, it is updated for all revisions.
  * This makes it trivial to support a feature like tagging, where you want to update all revisions of a document, including future versions.
1. Documents don't need to be JSON documents.  A "document" in FeatherDB lingo is any file of any HTTP Content-type. This has a few nice side-effects.  The FeatherDB server can now serve any type of file to any client.  For example, you can upload a binary image to the server, load it in your browser, and it renders correctly!  
  * No need to base64 encode a binary payload.  
  * Content-types are determined from the HTTP Content-type header.  
  * Everything is stored in binary format, unless the incoming HTTP Content-type has an associated handler.  
  * So far, there are handlers for:
    * text/html
    * text/plain
    * application/javascript (JSON)
    * image/{png,gif,jpeg}
  * Only JSON documents can be properly queried
  * A document's id can contain the '/' character.  This means that you can 'fake' an HTTP server's paths by being creative with your document ids (Similar in this respect to Amazon's S3).
1. Because the content of a document is no longer JSON, there is the need for a "Meta-data" JSON file to accompany the main file.  This meta-file is what stores the document's id, revision, etc...  For JSON only documents, the JSON data is stored directly in the meta-data entry.  For other documents, this is a separate file.  Information in this meta-file can be dynamically added by the content-type handler.  So, for example, the image handler can add width/height information to the record, and binary files can have their MD5/SHA1 sums calculated automatically on insert.
  * If you're keeping score, this means that using the default file-system backend, there are 2 files per revision for non-JSON documents, plus one more to store the common data.
1. The JavaScript views can query the database for associated documents.  Okay, so this actually is like "joining" documents.  Because the JavaScript engine is hosted in the same JVM, you can query the Java backend from the JavaScript view.  This lets you create joined views of your data, if needed


Those are the major differences, and things that I think should be in CouchDB.  As far as the status of FeatherDB, it has sat in SVN untouched for a few months, so I wanted to try and make it public, in case anyone else was interested.  AFAIK, it works.  However, there aren't any included libraries for accessing the server, so the API is a little undocumented.  It does follow the CouchDB API as closely as possible though.
You can access the project via SVN at: http://svn.fourspaces.com/public/featherdb/trunk

If you are interested in more information about it, leave a comment, or send me an email at: mbreese at fourspaces dot com.  Currently, the project requires Java 6 for the Scripting API support.
 
# Getting started

1 .Download the code via SVN from the above url
2. Run "ant run".
  This starts the server with the default configuration:
    * Port: 8889
    * Admin username: sa
    * Admin passowrd:password
    * Backend: File system (directory "testdb")
    * Allow anonymous access

  To access the server, connect to http://localhost:8889.

Important URIs 
    GET /_all_dbs -> list all databases
    GET /_auth -> authenticate
    You can authenticate via HTTP-Auth, or by passing the request parameters "username" and "password" (?username=sa&password=pass)
    If anonymous access is enabled, all requests are treated as if authenticated as an administrator

    GET /_invalidate -> invalidate the current credentials
    GET /_sessions -> show active authenticated sessions (not in anonymous mode)
    GET /_shutdown -> shutsdown the server (must be admin)

Databases
    GET /{dbnme} -> db stats
    PUT /{dbname} -> add a database (must be authenticated as admin) 
    DELETE /{dbname} -> remove a database (must be authenticated as admin)

Documents
    GET /{dbname}/{documentid} -> Get the document's current revision
    If this isn't found, but /db/docid/index is found, this will be returned instead

Optional parameters:
    showMeta=true -> returns the meta-information for the document (see above)
    showRevisions=true -> includes a list of available revisions in the meta-information

    GET /{dbname}/{documentid}/{revision} -> Get the document's content (see above)
    POST or PUT /{dbname}/{documentid} -> write the request's body as a new revision of the given document
    If the documentid doesn't exist, it is created

    POST /{dbname} -> write a new document, but use a generated id
    DELETE /{dbname}/{documentid} -> delete the document (must be able to write to db)

Views
    POST /{dbname}/_temp_view -> perform an adhoc query  
    The contents of the POST should be in the format of a javascript function
    Ex: 
    function(doc) { if (doc.value=='foo') {map(doc.id,doc.value); }}
    
    POST or PUT /{dbname}/viewname/functionname -> add/update a new view. 
    Note: the view name must start with an underscore ('_').

    The default functionname is "default"

    GET /{dbname}/_all_docs -> returns a list of all document ids
    This actually calls an included view named "_all_docs"

Views can be either written in either Java or JavaScript.  View documents are JSON documents that have the following attributes:
    'view_type': 'application/javascript' or 'java:fully.qualified.class.Name'

Java views must implement the interface: com.fourspaces.featherdb.views.View
JavaScript views are implemented as JSON documents in the format:

     { 
    'view_type': 'application/javascript',
    
    'view1': function(doc) {
    	// your code here
    	if (doc.val='foo') {
    		return doc; 
    	} ,
    'view2': function(doc) {
    	// your code here
    	if (doc.val='foo') {
    		map('key',doc.val); 
    	} 
    } 

So, as you see, JavaScript views are functions that take a JavaScript object as input, and either returns a JSON object, or builds a map with a key and value. (See CouchDB docs for more information).
There is another associated JavaScript method that you can call, and that is: function get(id,rev,db).  From JavaScript, you can retrieve other documents by id, revisions (optional), and database (optional).
Views are maintained by a "ViewRunner".  The ViewRunner is responsible for maintaining the index of results for documents in the database.  The only included ViewRunner doesn't store an index, but instead iterates over all of the documents in the database on each request.    
Configuration

FeatherDB is configured by writing a "featherdb.properties" file. In the src/java/com/fourspaces/featherdb directory, there is a 'default.properties' file that shows the default properties and their values.  Your featherdb.properties file will overwrite the default values.
 
# Additional Notes

If you are using an access mechanism that doesn't support cookies, you'll need to pass a "token" with each authenticated access.  This can be passed as a request parameter (?token=asdfasdf), or via an HTTP Header "FeatherDB-Token".  The token is given to you when you access "/_auth".  This isn't needed in anonymous access mode.
There is a severe lack of unit tests.  I'm sorry :).  Specifically, there is a lack of tests for running views, but it _should_ work.
I make no promises about this code.  If it works for you, great.  If not, let me know, and I'll see what I can do about it.  As always, patches are gratefully accepted. 
 
# Future directions

I'm not sure what to do with this project.  I'd prefer to not maintain a separate code base that may or may not track the CouchDB API.  I'd personally like to see the changes in the document structures implemented in CouchDB, but I'm not sure if it is possible given the split in a document data and meta data.  Then again, I'm completely unfamiliar with the CouchDB code base, so perhaps this will be possible.

I wanted to make this all public to see if anyone else may be interested in using this type of database and may be interested in helping to flesh it all out.

Hope you enjoy it! 

