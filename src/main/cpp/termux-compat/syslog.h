#pragma once
/* LibreSSL supplies reentrant syslog itself; do not rename its declarations
 * to Termux's differently typed inline Android polyfills. */
#include_next <syslog.h>
#undef syslog_r
#undef vsyslog_r
