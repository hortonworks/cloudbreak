#!py
import datetime

def run():
    date = datetime.datetime.now() + datetime.timedelta(minutes=__pillar__['ccm_jumpgate']['activation_in_minutes'])
    return {"restore_rotate_job": {
    "schedule.present": [
       {"function": "state.sls"},
       {"job_args": ["rotateccm.restoreoriginal"]},
       {"once": date.strftime("%Y/%m/%d, %H:%M:%S")},
       {"once_fmt": "%Y/%m/%d, %H:%M:%S"}
     ]
  }}

