@echo off
rem %cd%��ʾ��ǰĿ¼��ע��Ⱥ�ǰ��û�пո�
set folderName=%cd%
rem ���˵�ǰĿ¼�µ�jar�ļ���
for /f "delims=\" %%a in ('dir /b /a-d /o-d "%folderName%*.jar"') do (
  java -Xms1024m -Xmx1024m -jar %%a
  break
)
@pause