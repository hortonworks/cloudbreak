<html>
        <head>
            <style>
                ${style}
            </style>
            <title>Your cluster installation request completed with ${status} status</title>
        </head>
        <body id="page-home">
        <div class="fill-bg">

        </div>
        <nav class="navbar navbar-default navbar-fixed-top" role="navigation">
            <div class="container">

                <div class="navbar-header">
                    <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#navbar-collapsing">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <!-- .navbar-toggle -->
                    <a class="navbar-brand"> <span class="label label-success">${status}</span>
                    </a>
                </div>
                <!-- .navbar-header -->

                <div class="collapse navbar-collapse" id="navbar-collapsing">
                    <ul class="nav navbar-nav navbar-right">
                    </ul>
                    <!-- .navbar-nav -->
                </div>
                <!-- /.navbar-collapse -->

            </div>
            <!-- .container -->
        </nav>
        <section id="home-intro-1" style="padding-top: 60px !important">
            <div class="container">
                <div class="row container">

                    <!-- .col-md-6 -->
                    <div class="col-md-12">
                        <h2>Cluster Install Success</h2>

                        <p>Your cluster is ready to use. You can log into the ${server}:8080 Ambari UI using the configured username/password.</p>

                        </br>
                        Br,
                        </br>
                        The SequenceIQ Team


                    </div>
                    <!-- .col-md-6 -->
                </div>
                <!-- .row -->
            </div>
            <!-- .container -->
        </section>

        <footer>
            <div class="container">
                <div class="row">
                    <div class="col-xs-3">
                        <div class="row">

                        </div>
                        <!-- .row -->
                    </div>
                    <!-- .col- -->
                    <div class="col-xs-9">
                        <p class="text-right">SequenceIQ Inc. 2014. All rights reserved.
                        </p>
                    </div>
                    <!-- .col- -->
                </div>
                <!-- .row -->
            </div>
            <!-- .container -->
        </footer>

        </body>
</html>
