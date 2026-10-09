import os, pty, sys, time, select, json
def run(prog, inputs):
    pid, fd = pty.fork()
    if pid == 0:
        os.execv(prog, [prog])
    out = b''
    def drain(t=0.3):
        nonlocal out
        end = time.time() + t
        while time.time() < end:
            r, _, _ = select.select([fd], [], [], 0.05)
            if r:
                try:
                    d = os.read(fd, 4096)
                except OSError:
                    return False
                if not d: return False
                out += d
        return True
    drain()
    for line in inputs:
        os.write(fd, (line + '\n').encode())
        drain()
    drain(0.5)
    os.waitpid(pid, 0)
    return out.decode().replace('\r\n', '\n').rstrip('\n')
if __name__ == '__main__':
    print(run(sys.argv[1], sys.argv[2:]))
