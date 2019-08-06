#include "nc.h"

static FILE *log_fp;
pthread_mutex_t log_lock;

void
nc_log_start()
{
  nc_conf_rec conf_rec = {"log_file"};
  nc_conf_get(&conf_rec);
  log_fp = fopen(conf_rec.val, "a");
  pthread_mutex_init(&log_lock, NULL);
}

void
nc_log_write(const char *tag, const char *msg)
{
  time_t now;
  time(&now);

  pthread_mutex_lock(&log_lock);
  fprintf(log_fp, "%s[%s]: %s\n", ctime(&now), tag, msg);
  fflush(log_fp);
  pthread_mutex_unlock(&log_lock);
}

void
nc_do_log_writef(const char *tag, const char *msgf, va_list vargs)
{
  pthread_mutex_lock(&log_lock);
  vfprintf(log_fp, msgf, vargs);
  fprintf(log_fp, "\n");
  fflush(log_fp);
  va_end(vargs);
  pthread_mutex_unlock(&log_lock);
}

void
nc_log_writef(const char *tag, const char *msgf, ...)
{
  va_list vargs;
  int new_msgf_size;
  char time_str[NOW_STR_LEN];

  nc_utils_now_str(time_str);
  new_msgf_size = strlen(time_str) + strlen(tag) + strlen(msgf) + 6;

  char new_msgf[new_msgf_size];

  strcpy(new_msgf, time_str);
  strcat(new_msgf, " [");
  strcat(new_msgf, tag);
  strcat(new_msgf, "] ");
  strcat(new_msgf, msgf);

  va_start(vargs, msgf);
  nc_do_log_writef(tag, new_msgf, vargs);
}
