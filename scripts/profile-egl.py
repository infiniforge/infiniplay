"""Small EGL pbuffer frontend used only by generated native-core integration probes."""
import ctypes as C
RESET=C.CFUNCTYPE(None)
FRAMEBUFFER=C.CFUNCTYPE(C.c_size_t)
PROC=C.CFUNCTYPE(C.c_void_p,C.c_char_p)
class Render(C.Structure):
    _fields_=[('kind',C.c_int),('reset',RESET),('framebuffer',FRAMEBUFFER),('proc',PROC),
              ('depth',C.c_bool),('stencil',C.c_bool),('bottom_left',C.c_bool),
              ('major',C.c_uint),('minor',C.c_uint),('cache',C.c_bool),('destroy',RESET),('debug',C.c_bool)]
class Hardware:
    def __init__(self):
        self.egl=C.CDLL('/system/lib64/libEGL.so')
        p=C.c_void_p;i=C.c_int
        functions={'eglGetDisplay':([p],p),'eglInitialize':([p,C.POINTER(i),C.POINTER(i)],i),
            'eglChooseConfig':([p,C.POINTER(i),C.POINTER(p),i,C.POINTER(i)],i),
            'eglCreatePbufferSurface':([p,p,C.POINTER(i)],p),
            'eglCreateContext':([p,p,p,C.POINTER(i)],p),'eglMakeCurrent':([p,p,p,p],i),
            'eglGetProcAddress':([C.c_char_p],p),'eglDestroySurface':([p,p],i),
            'eglDestroyContext':([p,p],i),'eglTerminate':([p],i)}
        for name,(args,result) in functions.items():
            function=getattr(self.egl,name);function.argtypes=args;function.restype=result
        self.display=self.egl.eglGetDisplay(None)
        major=i();minor=i()
        assert self.egl.eglInitialize(self.display,C.byref(major),C.byref(minor)), 'EGL display unavailable'
        attrs=(i*19)(0x3033,1,0x3040,0x40,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3025,24,0x3026,8,0x3038,0,0)
        config=p();count=i()
        assert self.egl.eglChooseConfig(self.display,attrs,C.byref(config),1,C.byref(count)) and count.value,'No GLES3 pbuffer config'
        self.surface=self.egl.eglCreatePbufferSurface(self.display,config,(i*5)(0x3057,640,0x3056,480,0x3038))
        self.context=self.egl.eglCreateContext(self.display,config,None,(i*3)(0x3098,3,0x3038))
        assert self.surface and self.context and self.egl.eglMakeCurrent(self.display,self.surface,self.surface,self.context),'GLES context unavailable'
        self.framebuffer=FRAMEBUFFER(lambda:0)
        self.proc=PROC(lambda name:self.egl.eglGetProcAddress(name))
        self.render=None
    def install(self,data):
        render=C.cast(data,C.POINTER(Render)).contents
        if render.kind not in (2,4,5):return False
        render.framebuffer=self.framebuffer;render.proc=self.proc
        self.render=render
        return True
    def reset(self):
        assert self.render is not None,'Core did not negotiate hardware rendering'
        self.render.reset()
    def close(self):
        if self.render is not None and self.render.destroy:self.render.destroy()
    def release(self):
        self.egl.eglMakeCurrent(self.display,None,None,None)
        self.egl.eglDestroyContext(self.display,self.context)
        self.egl.eglDestroySurface(self.display,self.surface)
        self.egl.eglTerminate(self.display)
