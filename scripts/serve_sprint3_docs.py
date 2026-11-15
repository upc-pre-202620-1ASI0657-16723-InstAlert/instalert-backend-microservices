"""Separate local Swagger UIs; fixed Gateway proxy, no credentials persisted."""
import argparse,json,re,io,zipfile
from pathlib import Path
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from datetime import datetime,timezone,timedelta
ROOT=Path(__file__).resolve().parents[1]

def prepare_assets(service):
    """Extract pinned Swagger assets already bundled by springdoc in the built app.

    A clean checkout needs a packaged service, not an unversioned browser cache
    or a download of whatever Swagger version happens to be latest.
    """
    assets=ROOT/'target/swagger-assets'
    names=('swagger-ui.css','swagger-ui-bundle.js')
    jars=sorted((ROOT/'services'/('ms-'+service)/'target').glob('ms-*.jar'))
    if not jars:
        raise SystemExit('Package services/ms-'+service+' first (Build-LocalStack.ps1 or Maven package).')
    with zipfile.ZipFile(jars[-1]) as app:
        webjars=[name for name in app.namelist() if name.startswith('BOOT-INF/lib/swagger-ui-') and name.endswith('.jar')]
        if len(webjars)!=1:raise SystemExit('Expected one springdoc Swagger webjar in packaged application')
        with zipfile.ZipFile(io.BytesIO(app.read(webjars[0]))) as webjar:
            assets.mkdir(parents=True,exist_ok=True)
            for filename in names:
                candidates=[name for name in webjar.namelist() if name.endswith('/'+filename)]
                if len(candidates)!=1:raise SystemExit('Missing Swagger asset in webjar: '+filename)
                (assets/filename).write_bytes(webjar.read(candidates[0]))
    return assets
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--service',choices=['incidents','communities'],required=True)
    parser.add_argument('--port',type=int,required=True)
    args=parser.parse_args()
    contract=ROOT/'services'/('ms-'+args.service)/'openapi.json'
    assets=prepare_assets(args.service)
    html=f'''<!doctype html><html lang="es"><head><meta charset="utf-8"><title>InstAlert {args.service.title()} - Sprint 3</title><link rel="stylesheet" href="/swagger-ui.css"></head><body style="margin:0"><div style="padding:14px 24px;background:#edf4ff;font-family:Arial">InstAlert — {args.service.title()} · Sprint 3 · Solicitudes mediante API Gateway Docker (8085). Usar un JWT válido de Account en Authorize.</div><div id="swagger-ui"></div><script src="/swagger-ui-bundle.js"></script><script>SwaggerUIBundle({{url:'/openapi.json',dom_id:'#swagger-ui',deepLinking:true,validatorUrl:null,persistAuthorization:false,displayRequestDuration:true,defaultModelsExpandDepth:0}});</script></body></html>'''.encode()
    # Presentation-only redaction: actual request headers and responses are untouched.
    redaction=b'''<script>function redactCurl(){document.querySelectorAll('.curl-command code').forEach(function(e){const t=e.textContent;const safe=t.replace(/(Authorization: Bearer )[^'"\\r\\n]+/gi,'$1[TOKEN OCULTO]');if(safe!==t)e.textContent=safe;});}new MutationObserver(redactCurl).observe(document.getElementById('swagger-ui'),{childList:true,subtree:true,characterData:true});</script>'''
    html=html.replace(b'</body>',redaction+b'</body>')
    html=html.replace(b'</head>',b'<style>textarea.body-param__text{height:160px!important;min-height:160px!important}</style></head>')
    class Handler(BaseHTTPRequestHandler):
        def send(self,code,data,kind,extra=None):
            self.send_response(code);self.send_header('Content-Type',kind);self.send_header('Cache-Control','no-store')
            for k,v in (extra or {}).items():self.send_header(k,v)
            self.end_headers();self.wfile.write(data)
        def do_GET(self):
            path=self.path.split('?')[0]
            if path.startswith('/api/v1/'):return self.proxy()
            if path in ['/','/swagger-ui/index.html']:return self.send(200,html,'text/html; charset=utf-8')
            if path=='/openapi.json':
                spec=json.loads(contract.read_text(encoding='utf-8'))
                spec['servers']=[{'url':'/','description':'Gateway Docker mediante proxy local de documentación'}]
                for ops in spec['paths'].values():
                    for op in ops.values():
                        for media in op.get('requestBody',{}).get('content',{}).values():
                            example=media.get('example',{})
                            if 'expiresAt' in example:example['expiresAt']=(datetime.now(timezone.utc)+timedelta(hours=1)).isoformat()
                return self.send(200,json.dumps(spec,ensure_ascii=False).encode(),'application/json; charset=utf-8')
            allowed={'/swagger-ui.css':'text/css','/swagger-ui-bundle.js':'text/javascript'}
            if path in allowed:return self.send(200,(assets/path[1:]).read_bytes(),allowed[path])
            self.send(404,b'Not found','text/plain')
        def proxy(self):
            path=self.path.split('?')[0]
            spec=json.loads(contract.read_text(encoding='utf-8'))
            allowed=any(self.command.lower() in ops and re.fullmatch(re.sub(r'\\\{[^}]+\\\}',r'[^/]+',re.escape(template)),path) for template,ops in spec['paths'].items())
            if not allowed or any(x in path for x in ['..','%','\\']):return self.send(404,b'Unknown service operation','text/plain')
            try:length=int(self.headers.get('Content-Length','0'))
            except ValueError:return self.send(400,b'Invalid length','text/plain')
            if length<0 or length>6*1024*1024:return self.send(413,b'Request too large','text/plain')
            body=self.rfile.read(length) if length else None
            headers={k:self.headers[k] for k in ['Authorization','Content-Type','Accept'] if k in self.headers}
            request=Request('http://127.0.0.1:8085'+self.path,data=body,headers=headers,method=self.command)
            try:
                with urlopen(request,timeout=20) as response:
                    extra={k:response.headers[k] for k in ['Content-Disposition','X-Content-Type-Options'] if k in response.headers}
                    self.send(response.status,response.read(),response.headers.get('Content-Type','application/json'),extra)
            except HTTPError as e:self.send(e.code,e.read(),e.headers.get('Content-Type','application/json'))
            except URLError:self.send(502,b'{"error":"Gateway Docker unavailable on 8085"}','application/json')
        do_POST=proxy;do_PUT=proxy;do_PATCH=proxy;do_DELETE=proxy
        def log_message(self,format,*args):pass
    print(f'{args.service.title()} Swagger: http://localhost:{args.port}/swagger-ui/index.html',flush=True)
    ThreadingHTTPServer(('127.0.0.1',args.port),Handler).serve_forever()
if __name__=='__main__':main()
