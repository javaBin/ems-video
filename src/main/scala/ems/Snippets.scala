package ems

import scala.xml.NodeSeq

object Snippets {

  def page(base: String, body: NodeSeq): NodeSeq = {
      <html lang="en">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <meta name="description" content="Assign vimeo url"/>
          <meta name="author" content="Erlend Hamnaberg" />

          <title>EMS VIDEO</title>
          <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous"/>
          <style>
            {
              """
                | body {
                |   padding-top: 60px;
                | }
              """.stripMargin
            }
          </style>
        </head>

        <body>

          <nav class="navbar navbar-inverse navbar-fixed-top">
            <div class="container">
              <div class="navbar-header">
                <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
                  <span class="sr-only">Toggle navigation</span>
                  <span class="icon-bar"></span>
                  <span class="icon-bar"></span>
                  <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="#">EMS VIDEO</a>
              </div>
              <div id="navbar" class="collapse navbar-collapse">
                <ul class="nav navbar-nav">
                  <li><a href={s"$base/events"}>Events</a></li>
                </ul>
              </div><!--/.nav-collapse -->
            </div>
          </nav>

          <div class="container">
            {body}
          </div>

          <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
        </body>
      </html>
  }
}
