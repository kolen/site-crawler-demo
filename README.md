site-crawler-demo
=================

Site crawler and link extractor demo using Akka (with Java 8 stuff).

Crawls each domain from list, maximum 100 pages on each domain, collects unique links and outputs them to file.

Command-line options:

    -d   --domains   File with domains to crawl (one per line)
    -o   --output    Links output file
    -s   --summary   Summary output file
         --help      Display help
