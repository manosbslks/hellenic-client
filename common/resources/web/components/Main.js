import * as FallbackLoader from '/fallback-loader.js'
import { modules } from '/modules.js'
import * as http from '/http.js'
import * as events from '/events-service.js'
import { getComponent } from '/components/Loader.js'

const { ref, computed, watch, onMounted, onUnmounted } = await FallbackLoader.vue();

const reducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const button = getComponent('ModuleButton');

export function createComponent(template) {
    return {
        components: {
            ModuleButton: button
        },
        template,
        setup() {
            const automation = ref(modules.automation);
            const esp = ref(modules.esp);
            const hacks = ref(modules.hacks);
            const visuals = ref(modules.visuals);
            const scripting = ref(modules.scripting);
            const utility = ref(modules.utility);
            const groups = [
                { key: 'automation', name: 'Automation', icon: '⚙️', modules: modules.automation },
                { key: 'esp', name: 'ESP', icon: '\u{1F441}️', modules: modules.esp },
                { key: 'hacks', name: 'Hacks', icon: '⚡', modules: modules.hacks },
                { key: 'visuals', name: 'Visuals', icon: '✨', modules: modules.visuals },
                { key: 'scripting', name: 'Scripting', icon: '\u{1F4BB}', modules: modules.scripting },
                { key: 'utility', name: 'Utility', icon: '\u{1F9F0}', modules: modules.utility }
            ];
            const statuses = ref({});
            const generalInfo = ref({});
            const filtered = ref({});

            const onFilter = value => {
                const map = {};

                let words = value.split(/\s+/).filter(w => w).map(w => w.toLowerCase());
                if (words.length == 0) {
                    for (let module of modules.all) {
                        map[module.component] = true;
                    }
                } else {
                    for (let module of modules.all) {
                        const tags = module.tags;
                        if (words.every(w => tags.some(t => t.indexOf(w) >= 0))) {
                            map[module.component] = true;
                        }
                    }
                }

                filtered.value = map;
            };

            const onEvent = event => {
                if (event.type == 'filter') {
                    onFilter(event.value);
                }
            };

            const onCount = group => {
                return Object.keys(group).filter(component => statuses.value[component]).length;
            };

            const activeTotal = computed(() => Object.values(statuses.value).filter(Boolean).length);

            // general-information values come as "Label: value"
            const infoPart = (text, index) => {
                if (!text) {
                    return '—';
                }
                const at = text.indexOf(': ');
                return at < 0 ? text : (index == 0 ? text.slice(0, at) : text.slice(at + 2));
            };

            const stats = computed(() => {
                const info = generalInfo.value;
                return [
                    { num: modules.all.filter(m => m.path).length, lbl: 'Modules' },
                    { num: activeTotal.value, lbl: 'Active', grad: true },
                    { num: infoPart(info.gameVersion, 1), lbl: 'Minecraft' },
                    { num: infoPart(info.modLoaderVersion, 1), lbl: info.modLoaderVersion ? infoPart(info.modLoaderVersion, 0) : 'Loader' },
                    { num: infoPart(info.modCount, 1), lbl: 'Mods loaded' }
                ];
            });

            // numbers in the hero count up to their value instead of popping in
            const shown = ref({});
            const tweens = {};
            const countTo = (key, target) => {
                const from = typeof shown.value[key] == 'number' ? shown.value[key] : 0;
                if (reducedMotion || from == target) {
                    shown.value[key] = target;
                    return;
                }
                if (document.body.classList.contains('booting')) {
                    // start counting when the loader fades, not behind it
                    shown.value[key] = 0;
                    window.addEventListener('boot-done', () => countTo(key, target), { once: true });
                    return;
                }
                const token = tweens[key] = (tweens[key] || 0) + 1;
                const start = performance.now();
                const duration = 1100;
                const step = now => {
                    if (tweens[key] != token) {
                        return;
                    }
                    const p = Math.min(1, (now - start) / duration);
                    const eased = 1 - Math.pow(1 - p, 3);
                    shown.value[key] = Math.round(from + (target - from) * eased);
                    if (p < 1) {
                        requestAnimationFrame(step);
                    }
                };
                requestAnimationFrame(step);
            };

            watch(stats, list => {
                for (const stat of list) {
                    const value = Number(stat.num);
                    if (typeof stat.num == 'number' || (/^\d+$/.test(stat.num))) {
                        countTo(stat.lbl, value);
                    } else {
                        shown.value[stat.lbl] = stat.num;
                    }
                }
            }, { immediate: true });

            const onModuleClick = module => {
                window.location.hash = '#/' + module.path;
            };

            onMounted(() => {
                onFilter('');
                events.subscribe(onEvent);

                http.get('/api/modules-status').then(response => {
                    statuses.value = response;
                });
                http.get('/api/general-information').then(response => {
                    generalInfo.value = response;
                });
                events.trigger({
                    type: 'focus-filter'
                });
            });
            onUnmounted(() => {
                events.unsubscribe(onEvent);
            });

            return {
                automation,
                esp,
                hacks,
                visuals,
                scripting,
                utility,
                groups,
                statuses,
                generalInfo,
                activeTotal,
                stats,
                shown,
                filtered,

                onCount,
                onModuleClick
            };
        }
    };
}