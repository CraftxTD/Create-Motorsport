let SessionLoad = 1
let s:so_save = &g:so | let s:siso_save = &g:siso | setg so=0 siso=0 | setl so=-1 siso=-1
let v:this_session=expand("<sfile>:p")
doautoall SessionLoadPre
silent only
silent tabonly
cd ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block
if expand('%') == '' && !&modified && line('$') <= 1 && getline(1) == ''
  let s:wipebuf = bufnr('%')
endif
let s:shortmess_save = &shortmess
set shortmess+=aoO
badd +119 ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block/DownFlapBlock.java
badd +42 ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block/SteeringWheelBlock.java
badd +46 ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block/EngineBlock.java
badd +49 ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block/SuspensionBlock.java
badd +24 jar:///c\%3A/Users/andre/scoop/apps/gradle/9.3.1/.gradle/caches/modules-2/files-2.1/maven.modrinth/T9PomCSv/1L6XJqnY/f3ba9a497122ba29d9e94ebe6f9a3a3aaa5fa756/T9PomCSv-1L6XJqnY.jar!/dev/ryanhcode/sable/api/block/BlockSubLevelLiftProvider.class
badd +6 ~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json
argglobal
%argdel
edit ~/Documents/Projects/Create-Motorsport/src/main/java/com/createmotorsport/block/DownFlapBlock.java
let s:save_splitbelow = &splitbelow
let s:save_splitright = &splitright
set splitbelow splitright
wincmd _ | wincmd |
vsplit
1wincmd h
wincmd w
let &splitbelow = s:save_splitbelow
let &splitright = s:save_splitright
wincmd t
let s:save_winminheight = &winminheight
let s:save_winminwidth = &winminwidth
set winminheight=0
set winheight=1
set winminwidth=0
set winwidth=1
exe 'vert 1resize ' . ((&columns * 157 + 158) / 316)
exe 'vert 2resize ' . ((&columns * 158 + 158) / 316)
argglobal
balt ~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json
setlocal foldmethod=expr
setlocal foldexpr=v:lua.vim.lsp.foldexpr()
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=99
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
83
sil! normal! zo
95
sil! normal! zo
121
sil! normal! zo
128
sil! normal! zo
155
sil! normal! zo
223
sil! normal! zo
235
sil! normal! zo
287
sil! normal! zo
let s:l = 119 - ((16 * winheight(0) + 38) / 76)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 119
normal! 0
wincmd w
argglobal
if bufexists(fnamemodify("~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json", ":p")) | buffer ~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json | else | edit ~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json | endif
if &buftype ==# 'terminal'
  silent file ~/Documents/Projects/Create-Motorsport/src/main/resources/assets/createmotorsport/models/item/down_flap.json
endif
balt jar:///c\%3A/Users/andre/scoop/apps/gradle/9.3.1/.gradle/caches/modules-2/files-2.1/maven.modrinth/T9PomCSv/1L6XJqnY/f3ba9a497122ba29d9e94ebe6f9a3a3aaa5fa756/T9PomCSv-1L6XJqnY.jar!/dev/ryanhcode/sable/api/block/BlockSubLevelLiftProvider.class
setlocal foldmethod=expr
setlocal foldexpr=v:lua.vim.lsp.foldexpr()
setlocal foldmarker={{{,}}}
setlocal foldignore=#
setlocal foldlevel=99
setlocal foldminlines=1
setlocal foldnestmax=20
setlocal foldenable
1
sil! normal! zo
10
sil! normal! zo
67
sil! normal! zo
let s:l = 6 - ((5 * winheight(0) + 38) / 76)
if s:l < 1 | let s:l = 1 | endif
keepjumps exe s:l
normal! zt
keepjumps 6
normal! 0
wincmd w
exe 'vert 1resize ' . ((&columns * 157 + 158) / 316)
exe 'vert 2resize ' . ((&columns * 158 + 158) / 316)
tabnext 1
if exists('s:wipebuf') && len(win_findbuf(s:wipebuf)) == 0 && getbufvar(s:wipebuf, '&buftype') isnot# 'terminal'
  silent exe 'bwipe ' . s:wipebuf
endif
unlet! s:wipebuf
set winheight=1 winwidth=20
let &shortmess = s:shortmess_save
let &winminheight = s:save_winminheight
let &winminwidth = s:save_winminwidth
let s:sx = expand("<sfile>:p:r")."x.vim"
if filereadable(s:sx)
  exe "source " . fnameescape(s:sx)
endif
let &g:so = s:so_save | let &g:siso = s:siso_save
set hlsearch
nohlsearch
doautoall SessionLoadPost
unlet SessionLoad
" vim: set ft=vim :
